package haven;

public class Argon2 {
    private static final int SYNC_POINTS = 4;
    private static final int BLOCK_SIZE = 1024;
    private static final int PREHASH_DIGEST_LENGTH = 64;
    private static final int PREHASH_SEED_LENGTH = 72;
    private static final int ADDRESSES_IN_BLOCK = 128;
    private Type type;
    private int t, m, p;
    private Version version = Version.V13;

    public static enum Type {
	I(1), D(0), ID(2);

	public final int id;
	Type(int id) {this.id = id;}
    }

    public static enum Version {
	V10(0x10), V13(0x13);

	public final int id;
	Version(int id) {this.id = id;}
    }

    public Argon2(Type type, int t, int m, int p) {
	this.type = type;
	this.t = t;
	this.m = m;
	this.p = p;
    }

    public static class Block {
	public static final int N = BLOCK_SIZE / 8;
	public static final Block zero = new Block();
	public final long[] base;
	public final int off;

	public Block() {
	    this.base = new long[N];
	    this.off = 0;
	}

	public Block(long[] base, int off) {
	    this.base = base;
	    this.off = off;
	}

	public long get(int i) {
	    return(base[off + i]);
	}

	public void set(int i, long v) {
	    base[off + i] = v;
	}

	public void copy(Block src) {
	    for(int i = 0; i < N; i++)
		base[off + i] = src.base[src.off + i];
	}

	public void xor(Block src) {
	    for(int i = 0; i < N; i++)
		base[off + i] ^= src.base[src.off + i];
	}

	public void load(byte[] bytes) {
	    for(int i = 0, o = 0; i < N; i++, o += 8) {
		for(int b = 0, B = 0; b < 8; b++, B += 8)
		    base[off + i] |= (bytes[o + b] & 0xffL) << B;
	    }
	}

	public void store(byte[] bytes) {
	    for(int i = 0, o = 0; i < N; i++, o += 8) {
		for(int b = 0, B = 0; b < 8; b++, B += 8)
		    bytes[o + b] = (byte)(base[off + i] >> B);
	    }
	}

	public void dump() {
	    for(int i = 0; i < 8; i++) {
		for(int o = 0; o < 16; o++) {
		    System.err.printf("%16s ", Long.toHexString(base[off + (i * 16) + o]));
		}
		System.err.println();
	    }
	    System.err.println();
	}
    }

    public class Instance {
	private final int lanes, passes, m_cost;
	private final int outlen;
	private final int memory_blocks, segment_length, lane_length;
	private long[] memory;

	private Instance(int outlen) {
	    this.outlen = outlen;

	    this.lanes = Argon2.this.p;
	    this.passes = Argon2.this.t;
	    this.m_cost = Argon2.this.m;

	    int memory_blocks = Math.max(m_cost, 2 * SYNC_POINTS * lanes);
	    this.segment_length = memory_blocks / (lanes * SYNC_POINTS);
	    this.memory_blocks = segment_length * (lanes * SYNC_POINTS);
	    this.lane_length = segment_length * SYNC_POINTS;
	    memory = new long[memory_blocks * (BLOCK_SIZE / 8)];
	}

	private Block block(int i) {
	    return(new Block(memory, i * Block.N));
	}

	private void blake2b_long(byte[] out, int outoff, int outlen, byte[] in, int inoff, int inlen) {
	    byte[] outlen_bytes = new byte[4];
	    Utils.int32e(outlen, outlen_bytes, 0);
	    if(outlen <= Digest.BLAKE2B.diglen()) {
		System.arraycopy(new Blake2b(outlen).get().update(outlen_bytes).update(in, inoff, inlen).digest(), 0, out, outoff, outlen);
	    } else {
		byte[] out_buffer = Digest.BLAKE2B.get().update(outlen_bytes).update(in, inoff, inlen).digest();
		System.arraycopy(out_buffer, 0, out, outoff, out_buffer.length / 2);
		outoff += out_buffer.length / 2;
		outlen -= out_buffer.length / 2;
		while(outlen > Digest.BLAKE2B.diglen()) {
		    out_buffer = Digest.BLAKE2B.get().update(out_buffer).digest();
		    System.arraycopy(out_buffer, 0, out, outoff, out_buffer.length / 2);
		    outoff += out_buffer.length / 2;
		    outlen -= out_buffer.length / 2;
		}
		System.arraycopy(new Blake2b(outlen).get().update(out_buffer).digest(), 0, out, outoff, outlen);
	    }
	}

	private byte[] initial_hash(byte[] key, byte[] salt) {
	    Digest hash = new Blake2b(PREHASH_DIGEST_LENGTH).get();
	    byte[] value = new byte[4];
	    Utils.int32e(lanes,      value, 0); hash.update(value);
	    Utils.int32e(outlen,     value, 0); hash.update(value);
	    Utils.int32e(m_cost,     value, 0); hash.update(value);
	    Utils.int32e(passes,     value, 0); hash.update(value);
	    Utils.int32e(version.id, value, 0); hash.update(value);
	    Utils.int32e(type.id,    value, 0); hash.update(value);

	    Utils.int32e(key.length, value, 0); hash.update(value);
	    hash.update(key);
	    Utils.int32e(salt.length, value, 0); hash.update(value);
	    hash.update(salt);
	    Utils.int32e(0, value, 0); hash.update(value); // Secret, not currently supported
	    Utils.int32e(0, value, 0); hash.update(value); // AD, not currently supported

	    byte[] ret = new byte[PREHASH_SEED_LENGTH];
	    System.arraycopy(hash.digest(), 0, ret, 0, PREHASH_DIGEST_LENGTH);
	    return(ret);
	}

	private void fill_first_blocks(byte[] blockhash) {
	    byte[] blockhash_bytes = new byte[BLOCK_SIZE];
	    for(int l = 0; l < lanes; l++) {
		Utils.int32e(0, blockhash, PREHASH_DIGEST_LENGTH + 0);
		Utils.int32e(l, blockhash, PREHASH_DIGEST_LENGTH + 4);
		blake2b_long(blockhash_bytes, 0, BLOCK_SIZE, blockhash, 0, PREHASH_SEED_LENGTH);
		block((l * lane_length) + 0).load(blockhash_bytes);
		Utils.int32e(1, blockhash, PREHASH_DIGEST_LENGTH + 0);
		blake2b_long(blockhash_bytes, 0, BLOCK_SIZE, blockhash, 0, PREHASH_SEED_LENGTH);
		block((l * lane_length) + 1).load(blockhash_bytes);
	    }
	}

	private long fBlaMka(long x, long y) {
	    long m = 0xffffffffL;
	    long xy = (x & m) * (y & m);
	    return(x + y + (2 * xy));
	}

	private void G(Block B, int a, int b, int c, int d) {
	    B.set(a, fBlaMka(B.get(a), B.get(b)));
	    B.set(d, Long.rotateRight(B.get(d) ^ B.get(a), 32));
	    B.set(c, fBlaMka(B.get(c), B.get(d)));
	    B.set(b, Long.rotateRight(B.get(b) ^ B.get(c), 24));
	    B.set(a, fBlaMka(B.get(a), B.get(b)));
	    B.set(d, Long.rotateRight(B.get(d) ^ B.get(a), 16));
	    B.set(c, fBlaMka(B.get(c), B.get(d)));
	    B.set(b, Long.rotateRight(B.get(b) ^ B.get(c), 63));
	}

	private void BLAKE2_ROUND_NOMSG(Block B, int i0, int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14, int i15) {
	    G(B,  i0,  i4,  i8, i12);
	    G(B,  i1,  i5,  i9, i13);
	    G(B,  i2,  i6, i10, i14);
	    G(B,  i3,  i7, i11, i15);
	    G(B,  i0,  i5, i10, i15);
	    G(B,  i1,  i6, i11, i12);
	    G(B,  i2,  i7,  i8, i13);
	    G(B,  i3,  i4,  i9, i14);
	}

	private void fill_block(Block prev_block, Block ref_block, Block next_block, boolean with_xor) {
	    Block blockR = new Block(), block_tmp = new Block();
	    blockR.copy(ref_block);
	    blockR.xor(prev_block);
	    block_tmp.copy(blockR);
	    if(with_xor)
		block_tmp.xor(next_block);
	    for(int i = 0; i < 8; i++) {
		BLAKE2_ROUND_NOMSG(blockR,
				   (16 * i) +  0, (16 * i) +  1, (16 * i) +  2, (16 * i) +  3,
				   (16 * i) +  4, (16 * i) +  5, (16 * i) +  6, (16 * i) +  7,
				   (16 * i) +  8, (16 * i) +  9, (16 * i) + 10, (16 * i) + 11,
				   (16 * i) + 12, (16 * i) + 13, (16 * i) + 14, (16 * i) + 15);
	    }
	    for(int i = 0; i < 8; i++) {
		BLAKE2_ROUND_NOMSG(blockR,
				   (2 * i) +   0, (2 * i) +   1, (2 * i) +  16, (2 * i) +  17,
				   (2 * i) +  32, (2 * i) +  33, (2 * i) +  48, (2 * i) +  49,
				   (2 * i) +  64, (2 * i) +  65, (2 * i) +  80, (2 * i) +  81,
				   (2 * i) +  96, (2 * i) +  97, (2 * i) + 112, (2 * i) + 113);
	    }
	    next_block.copy(block_tmp);
	    next_block.xor(blockR);
	}

	private void next_addresses(Block address_block, Block input_block) {
	    input_block.set(6, input_block.get(6) + 1);
	    fill_block(Block.zero, input_block, address_block, false);
	    fill_block(Block.zero, address_block, address_block, false);
	}

	boolean b4;

	private int index_alpha(int pass, int slice, int lane, int index, long pseudo_rand, boolean same_lane) {
	    int reference_area_size;
	    if(pass == 0) {
		if(slice == 0) {
		    reference_area_size = index - 1;
		} else {
		    if(same_lane)
			reference_area_size = (slice * segment_length) + index - 1;
		    else
			reference_area_size = (slice * segment_length) + ((index == 0) ? -1 : 0);
		}
	    } else {
		if(same_lane)
		    reference_area_size = lane_length - segment_length + index - 1;
		else
		    reference_area_size = lane_length - segment_length + ((index == 0) ? -1 : 0);
	    }
	    long relative_position = pseudo_rand;
	    relative_position = (relative_position * relative_position) >>> 32;
	    relative_position = reference_area_size - 1 - ((reference_area_size * relative_position) >>> 32);
	    int start_position = 0;
	    if(pass != 0)
		start_position = (slice == SYNC_POINTS - 1) ? 0 : ((slice + 1) * segment_length);
	    int absolute_position = (int)Long.remainderUnsigned(start_position + relative_position, lane_length);
	    return(absolute_position);
	}

	private void fill_segment(int pass, int slice, int lane) {
	    boolean data_independent_addressing = (type == Type.I) ||
		((type == Type.ID) && (pass == 0) && (slice < (SYNC_POINTS / 2)));
	    Block input_block = data_independent_addressing ? new Block() : null;
	    Block address_block = data_independent_addressing ? new Block() : null;
	    if(data_independent_addressing) {
		input_block.set(0, pass);
		input_block.set(1, lane);
		input_block.set(2, slice);
		input_block.set(3, memory_blocks);
		input_block.set(4, passes);
		input_block.set(5, type.id);
	    }

	    int starting_index = 0;
	    if((pass == 0) && (slice == 0)) {
		starting_index = 2;
		if(data_independent_addressing) {
		    next_addresses(address_block, input_block);
		}
	    }

	    int curr_offset = (lane * lane_length) + (slice * segment_length) + starting_index, prev_offset;
	    if((curr_offset % lane_length) == 0)
		prev_offset = curr_offset + lane_length - 1;
	    else
		prev_offset = curr_offset - 1;

	    for(int i = starting_index; i < segment_length; i++, curr_offset++, prev_offset++) {
		if((curr_offset % lane_length) == 1)
		    prev_offset = curr_offset - 1;
		long pseudo_rand;
		if(data_independent_addressing) {
		    if((i % ADDRESSES_IN_BLOCK) == 0)
			next_addresses(address_block, input_block);
		    pseudo_rand = address_block.get(i % ADDRESSES_IN_BLOCK);
		} else {
		    pseudo_rand = block(prev_offset).get(0);
		}

		int ref_lane = Integer.remainderUnsigned((int)(pseudo_rand >>> 32), lanes);
		if((pass == 0) && (slice == 0))
		    ref_lane = lane;

		int ref_index = index_alpha(pass, slice, lane, i, pseudo_rand & 0xffffffffL, ref_lane == lane);
		Block ref_block = block((lane_length * ref_lane) + ref_index);
		Block curr_block = block(curr_offset);
		if(version == Version.V10) {
		    fill_block(block(prev_offset), ref_block, curr_block, false);
		} else {
		    if(pass == 0)
			fill_block(block(prev_offset), ref_block, curr_block, false);
		    else
			fill_block(block(prev_offset), ref_block, curr_block, true);
		}
	    }
	}

	private byte[] phinalize() {
	    Block blockhash = new Block();
	    blockhash.copy(block(lane_length - 1));
	    for(int l = 1; l < lanes; l++) {
		int last_block_in_lane = (l * lane_length) + (lane_length - 1);
		blockhash.xor(block(last_block_in_lane));
	    }
	    byte[] blockhash_bytes = new byte[BLOCK_SIZE];
	    blockhash.store(blockhash_bytes);
	    byte[] ret = new byte[outlen];
	    blake2b_long(ret, 0, outlen, blockhash_bytes, 0, BLOCK_SIZE);
	    return(ret);
	}

	private void fill_memory_blocks() {
	    for(int r : Utils.range(passes)) {
		for(int s : Utils.range(SYNC_POINTS)) {
		    Utils.range(lanes).parallelStream().forEach(l -> fill_segment(r, s, l));
		}
	    }
	}

	public byte[] hash(byte[] key, byte[] salt) {
	    byte[] blockhash = initial_hash(key, salt);
	    fill_first_blocks(blockhash);
	    fill_memory_blocks();
	    return(phinalize());
	}
    }

    public byte[] hash(byte[] key, byte[] salt, int len) {
	return(new Instance(len).hash(key, salt));
    }
}

package haven;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;

public class TextureAtlas implements Disposable {
    private final static int PADDING = 1;

    private final TexGL tex;
    private Coord next;
    private int maxRowHeight;

    public TextureAtlas(int width, int height) {
        this(new Coord(width, height));
    }

    public TextureAtlas(Coord size) {
        tex = new TexGL(size) {
            protected void fill(GOut g) {
                // initialize empty texture
                g.gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, tdim.x, tdim.y, 0,  GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
            }
        };
        next = new Coord(0, 0);
    }

    public Region add(BufferedImage image) {
        Region region = add(image.getWidth(), image.getHeight());
        region.update(image);
        return region;
    }

    public Region add(int width, int height) {
        if (next.y + height > tex.sz().y)
            throw new RuntimeException("Couldn't allocate region on the texture");
        if (next.x + width > tex.sz().x) {
            // allocate space in the next row
            next.x = 0;
            next.y += maxRowHeight + PADDING;
            maxRowHeight = 0;
        }

        Coord c = new Coord(next);

        next.x += width + PADDING;
        if (height > maxRowHeight)
            maxRowHeight = height;

        return new Region(c, new Coord(width, height));
    }

    @Override
    public void dispose() {
        tex.dispose();
    }

    public class Region extends TexSI {
        private boolean dirty;
        private BufferedImage image;

        public Region(Coord c, Coord size) {
            super(tex, c, size);
        }

        public void update(BufferedImage value) {
            if (value != null) {
                dirty = true;
                image = value;
            }
        }

        private void fill(GOut g) {
            if (image == null)
                return;

            BGL gl = g.gl;
            int format = TexI.detectfmt(image);
            byte[] pixels;
            int width;
            int height;
            switch (format) {
                case GL.GL_RGBA:
                case GL.GL_BGRA:
                    pixels = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
                    width = image.getWidth();
                    height = image.getHeight();
                    break;
                case GL.GL_RGB:
                case GL2.GL_BGR:
                    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
                    pixels = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
                    width = image.getWidth();
                    height = image.getHeight();
                    break;
                default:
                    format = GL.GL_RGBA;
                    width = sz().x;
                    height = sz().y;
                    pixels = TexI.convert(image, sz());
                    break;
            }

            gl.glBindTexture(GL.GL_TEXTURE_2D, tex.glid(g));
            gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, ul.x, ul.y, width, height, format, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels));
            GOut.checkerr(gl);
            
            dirty = false;
            // image is no longer needed
            image = null;
        }

        @Override
        public void render(GOut g, Coord c, Coord ul, Coord br, Coord sz) {
            if (dirty)
                fill(g);
            super.render(g, c, ul, br, sz);
        }

        @Override
        public void renderquad(GOut g, Coord ul, Coord bl, Coord br, Coord ur) {
            if (dirty)
                fill(g);
            super.render(g, ul, bl, br, ur);
        }
    }
}

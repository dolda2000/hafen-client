/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.util.*;
import java.io.*;
import java.nio.file.*;
import haven.Steam.UGItem;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUGC.ItemUpdateStatus;

public class SteamWorkshop {
    private static void err(String format, Object... args) {
	System.err.printf(format + "\n", args);
	System.exit(1);
    }

    private static void usage_upload(PrintStream out) {
	out.println("usage: haven.SteamWorkshop upload [-hq] CLIENT-DIRECTORY [MESSAGE]");
    }

    public static void cmd_upload(String[] args) {
	PosixArgs opt = PosixArgs.getopt(args, "h");
	if(opt == null) {
	    usage_upload(System.err);
	    System.exit(1);
	}
	boolean quiet = false;
	for(char c : opt.parsed()) {
	    switch(c) {
	    case 'h':
		usage_upload(System.out);
		System.exit(0);
		break;
	    case 'q':
		quiet = true;
		break;
	    }
	}
	if(opt.rest.length < 1) {
	    usage_upload(System.err);
	    System.exit(1);
	}
	Path dir = Utils.path(opt.rest[0]);
	if(!Files.exists(dir) || !Files.isDirectory(dir))
	    err("upload: %s: not a directory", dir);
	Path pfile = dir.resolve("workshop-client.properties");
	if(!Files.exists(pfile))
	    err("upload: %s: lacks workshop-client.properties files", dir);
	Properties props = new Properties();
	try(InputStream fp = Files.newInputStream(pfile)) {
	    props.load(fp);
	} catch(IOException e) {
	    e.printStackTrace();
	    System.exit(1);
	}
	Steam api = Steam.get();
	if(api == null)
	    err("upload: could not initialize steam (SteamAppID environment variable missing?)");
	UGItem item;
	if(props.containsKey("workshop-id")) {
	    item = api.ugitem(Long.parseLong(props.getProperty("workshop-id")));
	} else {
	    try {
		item = api.mkugitem();
	    } catch(InterruptedException e) {
		throw(new RuntimeException(e));
	    }
	    System.err.println("upload: note: creating new item");
	    System.err.println("add the following line to workshop-client.properties to update this entry in the future:");
	    System.err.printf("workshop-id=%d\n", item.fid());
	}
	UGItem.Update update = item.new Update();
	String prop;
	update.tags("Client");
	update.contents(dir);
	if((prop = props.getProperty("name")) == null)
	    err("upload: %s: lacks name property\n", pfile);
	update.title(prop);
	if((prop = props.getProperty("description-file")) != null) {
	    try {
		update.description(new String(Files.readAllBytes(dir.resolve(prop)), Utils.utf8));
	    } catch(NoSuchFileException e) {
		err("upload: description file %s: no such file", prop);
	    } catch(IOException e) {
		throw(new RuntimeException(e));
	    }
	} else if((prop = props.getProperty("description")) != null) {
	    update.description(prop);
	} else {
	    err("upload: %s: lacks description or description-file property\n", pfile);
	}
	if((prop = props.getProperty("preview-image")) == null)
	    err("upload: %s: lacks preview-image property\n", pfile);
	Path pvf = dir.resolve(prop);
	if(!Files.exists(pvf))
	    err("upload: preview file %s: no such file", pvf);
	update.preview(pvf);
	if((prop = props.getProperty("visibility")) == null)
	    err("upload: %s: lacks visibility property\n", pfile);
	switch(prop) {
	case "private": update.setprivate(); break;
	case "friends": update.setfriendsonly(); break;
	case "public": update.setpublic(); break;
	default:
	    err("upload: visibility is not one of `private', `friends' or `public': %s", prop);
	}
	update.submit(opt.rest.length > 1 ? opt.rest[1] : null);
	ItemUpdateStatus state = null;
	while(update.done == null) {
	    update.getprogress();
	    if(update.state != state) {
		if(!quiet && update.state != ItemUpdateStatus.Invalid)
		    System.err.println("upload: update state: " + update.state);
		state = update.state;
	    }
	    if((state == ItemUpdateStatus.UploadingContent) || (state == ItemUpdateStatus.UploadingPreviewFile)) {
		if(!quiet && (update.size > 0))
		    System.err.printf("upload: progress: %,d/%,d\n", update.prog, update.size);
	    }
	    try {
		Thread.sleep(100);
	    } catch(InterruptedException e) {
		throw(new RuntimeException(e));
	    }
	}
	if(update.done != SteamResult.OK)
	    err("upload: submission failure: " + update.done);
	if(!update.agreed)
	    System.err.println("upload: note: you need to agree to the Steam Workshop Legal Agreement to make your item public");
    }

    public static void main(String[] args) {
	String cmd = args[0].intern();
	if(cmd == "upload") {
	    cmd_upload(Utils.splice(args, 1));
	} else {
	    System.err.println("invalid workshop command: " + cmd);
	    System.exit(1);
	}
    }
}

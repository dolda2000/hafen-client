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

public class RemoteUI implements UI.Receiver, UI.Runner {
    public final Session sess;

    public RemoteUI(Session sess) {
	this.sess = sess;
	Widget.initnames();
    }

    public void rcvmsg(int id, String name, Object... args) {
	PMessage msg = new PMessage(RMessage.RMSG_WDGMSG);
	msg.addint32(id);
	msg.addstring(name);
	msg.addlist(args);
	sess.queuemsg(msg);
    }

    public static class Return extends PMessage {
	public final Session ret;

	public Return(Session ret) {
	    super(-1);
	    this.ret = ret;
	}
    }

    private void sendua(String key, String val) {
	PMessage msg = new PMessage(RMessage.RMSG_USERAGENT);
	msg.addstring(key).addstring(val);
	sess.queuemsg(msg);
    }

    private void sendua(UI ui) {
	try {
	    sendua("conf.id", Config.confid);
	    sendua("java.vm", Utils.getprop("java.vm.name", ""));
	    sendua("java.version", Utils.getprop("java.version", ""));
	    sendua("os.name", Utils.getprop("os.name", ""));
	    sendua("os.arch", Utils.getprop("os.arch", ""));
	    sendua("os.version", Utils.getprop("os.version", ""));
	    sendua("mem.heap", String.valueOf(Runtime.getRuntime().maxMemory()));
	    sendua("cpu.num", String.valueOf(Runtime.getRuntime().availableProcessors()));
	    sendua("ui.scale", String.format("%.2f", UI.scale(1.0)));
	    haven.render.Environment env = ui.getenv();
	    if(env != null) {
		sendua("render.env", env.getClass().getSimpleName());
		sendua("render.vendor", env.caps().vendor());
		sendua("render.device", env.caps().device());
		sendua("render.driver", env.caps().driver());
	    }
	} catch(Exception e) {
	    new Warning(e).issue();
	}
    }

    public void ret(Session sess) {
	this.sess.postuimsg(new Return(sess));
    }

    public UI.Runner run(UI ui) throws InterruptedException {
	try {
	    ui.setreceiver(this);
	    sendua(ui);
	    while(true) {
		PMessage msg = sess.getuimsg();
		if(msg == null) {
		    return(null);
		} else if(msg instanceof Return) {
		    sess.close();
		    return(new RemoteUI(((Return)msg).ret));
		} else if(msg.type == RMessage.RMSG_NEWWDG) {
		    int id = msg.int32();
		    String type = msg.string();
		    int parent = msg.int32();
		    Object[] pargs = msg.list(sess.resmapper);
		    Object[] cargs = msg.list(sess.resmapper);
		    ui.newwidgetp(id, type, parent, pargs, cargs);
		} else if(msg.type == RMessage.RMSG_WDGMSG) {
		    int id = msg.int32();
		    String name = msg.string();
		    ui.uimsg(id, name, msg.list(sess.resmapper));
		} else if(msg.type == RMessage.RMSG_DSTWDG) {
		    int id = msg.int32();
		    ui.destroy(id);
		} else if(msg.type == RMessage.RMSG_ADDWDG) {
		    int id = msg.int32();
		    int parent = msg.int32();
		    Object[] pargs = msg.list(sess.resmapper);
		    ui.addwidget(id, parent, pargs);
		} else if(msg.type == RMessage.RMSG_WDGBAR) {
		    Collection<Integer> deps = new ArrayList<>();
		    while(!msg.eom()) {
			int dep = msg.int32();
			if(dep == -1)
			    break;
			deps.add(dep);
		    }
		    Collection<Integer> bars = deps;
		    if(!msg.eom()) {
			bars = new ArrayList<>();
			while(!msg.eom()) {
			    int bar = msg.int32();
			    if(bar == -1)
				break;
			    bars.add(bar);
			}
		    }
		    ui.wdgbarrier(deps, bars);
		}
	    }
	} finally {
	    sess.close();
	    while(sess.getuimsg() != null);
	}
    }

    public void init(UI ui) {
	ui.sess = sess;
    }

    public String title() {
	return(sess.username);
    }
}

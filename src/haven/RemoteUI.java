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

public class RemoteUI implements UI.Receiver {
    Session sess;
    UI ui;
	
    public RemoteUI(Session sess) {
	this.sess = sess;
	Widget.initbardas();
    }
	
    public void rcvmsg(int id, String name, Object... args) {
	Message msg = new Message(Message.RMSG_WDGMSG);
	msg.adduint16(id);
	msg.addstring(name);
	msg.addlist(args);
	sess.queuemsg(msg);
    }
	
    public void run(UI ui) throws InterruptedException {
	this.ui = ui;
	ui.setreceiver(this);
	while(sess.alive()) {
	    Message msg;
	    while((msg = sess.getuimsg()) != null) {
		if(msg.type == Message.RMSG_NEWWDG) {
		    int id = msg.uint16();
		    String type = msg.string();
		    int parent = msg.uint16();
		    Object[] pargs = msg.list();
		    Object[] cargs = msg.list();
		    ui.newwidget(id, type, parent, pargs, cargs);
		} else if(msg.type == Message.RMSG_WDGMSG) {
		    int id = msg.uint16();
		    String name = msg.string();
		    ui.uimsg(id, name, msg.list());
		} else if(msg.type == Message.RMSG_DSTWDG) {
		    int id = msg.uint16();
		    ui.destroy(id);
		}
	    }
	    synchronized(sess) {
		sess.wait();
	    }
	}
    }
}

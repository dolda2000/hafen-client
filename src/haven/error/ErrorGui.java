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

package haven.error;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public abstract class ErrorGui extends JDialog implements ErrorStatus {
    private JLabel status;
    private JEditorPane info;
    private JPanel details;
    private JButton closebtn, detbtn;
    private JTextArea exbox;
    private JScrollPane infoc, exboxc;
    private Thread reporter;
    private boolean done;
    
    public ErrorGui(java.awt.Frame parent) {
	super(parent, "Haven error!", true);
	setMinimumSize(new Dimension(300, 100));
	setResizable(false);
	add(new JPanel() {{
	    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
	    add(new JLabel("An error has occurred!"));
	    add(status = new JLabel("Please wait..."));
	    add(infoc = new JScrollPane(info = new JEditorPane() {{
		setEditable(false);
		addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent ev) {
			    if(ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				try {
				    Desktop.getDesktop().browse(ev.getURL().toURI());
				} catch(Exception e) {
				    throw(new RuntimeException(e));
				}
			    } else if(ev.getEventType() == HyperlinkEvent.EventType.ENTERED) {
				setCursor(new Cursor(Cursor.HAND_CURSOR));
			    } else if(ev.getEventType() == HyperlinkEvent.EventType.EXITED) {
				setCursor(null);
			    }
			}
		    });
	    }}) {{
		setPreferredSize(new Dimension(300, 100));
		setVisible(false);
	    }});
	    add(new JPanel() {{
		setLayout(new FlowLayout());
		setAlignmentX(0);
		add(closebtn = new JButton("Close") {{
		    addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent ev) {
				ErrorGui.this.dispose();
				synchronized(ErrorGui.this) {
				    done = true;
				    ErrorGui.this.notifyAll();
				}
			    }
			});
		}});
		add(detbtn = new JButton("Details >>>") {{
		    addActionListener(new ActionListener() {
			    public void actionPerformed(ActionEvent ev) {
				if(details.isVisible()) {
				    details.setVisible(false);
				    detbtn.setText("Details >>>");
				} else {
				    details.setVisible(true);
				    detbtn.setText("<<< Details");
				}
				ErrorGui.this.pack();
			    }
			});
		}});
	    }});
	    add(details = new JPanel() {{
		setLayout(new BorderLayout());
		setAlignmentX(0);
		setVisible(false);
		add(exboxc = new JScrollPane(exbox = new JTextArea(15, 80) {{
		    setEditable(false);
		}}) {{
		    setVisible(true);
		}});
	    }});
	}});
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent ev) {
		    ErrorGui.this.dispose();
		    synchronized(ErrorGui.this) {
			done = true;
			ErrorGui.this.notifyAll();
		    }
		    reporter.interrupt();
		}
	    });
	pack();
    }
    
    public boolean goterror(Throwable t) {
	reporter = Thread.currentThread();
	java.io.StringWriter w = new java.io.StringWriter();
	t.printStackTrace(new java.io.PrintWriter(w));
	final String tr = w.toString();
	SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    closebtn.setEnabled(false);
		    status.setText("Please wait...");
		    exbox.setText(tr);
		    pack();
		    setVisible(true);
		}
	    });
	return(true);
    }
	
    public void connecting() {
	SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    status.setText("Connecting to server...");
		    pack();
		}
	    });
    }
	
    public void sending() {
	SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    status.setText("Sending error...");
		    pack();
		}
	    });
    }
	
    public void done(final String ctype, final String info) {
	done = false;
	SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    closebtn.setEnabled(true);
		    if((ctype != null) && ctype.equals("text/x-report-info")) {
			status.setText("There is information available about this error:");
			ErrorGui.this.info.setContentType("text/html");
			ErrorGui.this.info.setText(info);
			infoc.setVisible(true);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
				    infoc.getVerticalScrollBar().setValue(0);
				}
			    });
		    } else {
			status.setText("The error has been reported.");
		    }
		    pack();
		}
	    });
	synchronized(this) {
	    try {
		while(!done)
		    wait();
	    } catch(InterruptedException e) {
		throw(new Error(e));
	    }
	}
	errorsent();
    }
	
    public void senderror(Exception e) {
	final String errstr;
	if(e instanceof ReportException) {
	    /* C¦ */
	    StringBuilder buf = new StringBuilder();
	    buf.append("<html>");
	    String msg = e.getMessage();
	    for(int i = 0; i < msg.length(); i++) {
		char c = msg.charAt(i);
		if(c == '\n')
		    buf.append("<br>");
		else if(c == '<')
		    buf.append("&lt;");
		else if(c == '>')
		    buf.append("&gt;");
		else if(c == '&')
		    buf.append("&amp;");
		else
		    buf.append(c);
	    }
	    buf.append("</html>");
	    errstr = buf.toString();
	} else {
	    e.printStackTrace();
	    errstr = "An error occurred while sending!";
	}
	done = false;
	SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    closebtn.setEnabled(true);
		    status.setText(errstr);
		    pack();
		}
	    });
	synchronized(this) {
	    try {
		while(!done)
		    wait();
	    } catch(InterruptedException e2) {
		throw(new Error(e2));
	    }
	}
	errorsent();
    }
	
    public abstract void errorsent();
}

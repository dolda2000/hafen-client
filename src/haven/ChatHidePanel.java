package haven;

import java.awt.*;

public class ChatHidePanel extends Widget {
    private static final Tex bg = Resource.loadtex("gfx/hud/lbtn-upbg");

    private final ChatUI chat;
    private final IButton button;

    public ChatHidePanel(ChatUI chat) {
        this.chat = chat;
        this.button = add(new ChatHideButton(), 2, 6);
        resize(bg.sz());
    }

    @Override
    public void draw(GOut g) {
        g.image(bg, Coord.z);
        super.draw(g);
    }

    private class ChatHideButton extends IButton {
        private final Tex glow;

        public ChatHideButton() {
            super("gfx/hud/hb-btn-chat", "", "-d", "-h");
            this.glow = new TexI(PUtils.rasterimg(PUtils.blurmask(up.getRaster(), 2, 2, Color.WHITE)));
            this.tooltip = RichText.render("Hide/show chat ($col[255,255,0]{Ctrl+C})", 0);
        }

        public void click() {
            if (chat.visible) {
                chat.resize(chat.sz.x, 0);
                chat.hide();
            } else {
                chat.resize(chat.savedw, chat.savedh);
                chat.show();
                setfocus(chat);
            }
            Utils.setprefb("chatvis", chat.visible);
        }

        public void draw(GOut g) {
            super.draw(g);
            Color urg = ChatUI.urgcols[chat.urgency];
            if (urg != null) {
                GOut g2 = g.reclipl(new Coord(-2, -2), g.sz.add(4, 4));
                g2.chcolor(urg.getRed(), urg.getGreen(), urg.getBlue(), 128);
                g2.image(glow, Coord.z);
            }
        }
    }
}

package haven;

import java.awt.*;
import java.awt.image.ImageObserver;

public class GraphicsWrapper {
    private final java.awt.Graphics impl;

    public GraphicsWrapper(java.awt.Graphics impl) {
	this.impl = impl;
    }

    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
	return impl.drawImage(img, x, y, observer);
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
	return impl.drawImage(img, x, y, width, height, observer);
    }

    public boolean drawImage(ScaledBufferedImage img, int x, int y, ImageObserver observer) {
	return impl.drawImage(img.get(), x, y, img.getWidth(), img.getHeight(), observer);
    }

    public boolean drawImage(ScaledBufferedImage img, int x, int y, int width, int height, ImageObserver observer) {
	return impl.drawImage(img.get(), x, y, width, height, observer);
    }

    public void dispose() {
	impl.dispose();
    }
}

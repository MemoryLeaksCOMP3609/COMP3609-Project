import java.awt.Graphics2D;

// Base interface for image effects.

public interface ImageFX {
    public void update();
    public void draw(Graphics2D g2d);
}

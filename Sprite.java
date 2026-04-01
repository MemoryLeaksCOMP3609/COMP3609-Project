import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;

// Base sprite class with position and size
public class Sprite {
    
    protected JPanel panel;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected Image image;
    
    public Sprite(JPanel p, int xPos, int yPos, int w, int h) {
        panel = p;
        x = xPos;
        y = yPos;
        width = w;
        height = h;
        image = null;
    }
    
    public void draw(Graphics2D g2) {

    }
    
    public Rectangle2D.Double getBoundingRectangle() {
        return new Rectangle2D.Double(x, y, width, height);
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
}

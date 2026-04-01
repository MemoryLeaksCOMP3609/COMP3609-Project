import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class SolidObject {
    
    private int x;
    private int y;
    private int width;
    private int height;
    private Rectangle2D.Double bounds;
    private BufferedImage image;
    
    // Constructor for  solid objects. width and height are derived from the image dimensions.
    public SolidObject(double xPos, double yPos, BufferedImage img) {
        x = (int) xPos;
        y = (int) yPos;
        image = img;
        
        if (image != null) {
            width = image.getWidth();
            height = image.getHeight();
        } else {
            width = 0;
            height = 0;
        }
        
        bounds = new Rectangle2D.Double(x, y, width, height);
    }
    
    // Draws the solid object image at the specified offset.
    public void draw(Graphics2D g2, int offsetX, int offsetY) {
        int drawX = x - offsetX;
        int drawY = y - offsetY;
        
        if (image != null) {
            g2.drawImage(image, drawX, drawY, null);
        }
    }
    
    public Rectangle2D.Double getBoundingRectangle() {
        return bounds;
    }
}

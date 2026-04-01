import java.awt.Graphics2D;
import java.awt.image.BufferedImage;


// Fade/disappear effect.
public class DisappearFX implements ImageFX {
    
    private int x;
    private int y;
    private int width;
    private int height;
    private BufferedImage originalImage;
    private BufferedImage currentImage;
    private boolean active;
    
    public DisappearFX(int xPos, int yPos, int w, int h, String imagePath) {
        x = xPos;
        y = yPos;
        width = w;
        height = h;
        active = true;
        
        originalImage = ImageManager.loadBufferedImage(imagePath);
        if (originalImage != null) {
            currentImage = ImageManager.copyImage(originalImage);
        }
    }
    
    @Override
    public void update() {
        if (!active) return;        
    }
    
    @Override
    public void draw(Graphics2D g2) {
        if (!active || currentImage == null) return;
        g2.drawImage(currentImage, x, y, width, height, null);
    }
    
    public void setPosition(int xPos, int yPos) {
        x = xPos;
        y = yPos;
    }

    public BufferedImage getCurrentImage() {
        return currentImage;
    }
}

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class GrayScaleFX implements ImageFX {
    
    private int x;
    private int y;
    private int width;
    private int height;
    private BufferedImage originalImage;
    private BufferedImage grayImage;
    private boolean active;
    
    // Load image from file path and convert to grayscale
    public GrayScaleFX(int xPos, int yPos, int w, int h, String imagePath) {
        x = xPos;
        y = yPos;
        width = w;
        height = h;
        active = true;
        
        originalImage = ImageManager.loadBufferedImage(imagePath);
        if (originalImage != null) {
            grayImage = convertToGray(ImageManager.copyImage(originalImage));
        }
    }
    
    // Use pre-converted grayscale image
    public GrayScaleFX(int xPos, int yPos, int w, int h, BufferedImage sourceImage, BufferedImage grayscaleImage) {
        x = xPos;
        y = yPos;
        width = w;
        height = h;
        active = true;
        originalImage = sourceImage;
        grayImage = grayscaleImage;
    }
    
    private BufferedImage convertToGray(BufferedImage src) {
        if (src == null) return null;
        
        int imWidth = src.getWidth();
        int imHeight = src.getHeight();
        
        int[] pixels = new int[imWidth * imHeight];
        src.getRGB(0, 0, imWidth, imHeight, pixels, 0, imWidth);
        
        for (int i = 0; i < pixels.length; i++) {
            int alpha = (pixels[i] >> 24) & 255;
            int red = (pixels[i] >> 16) & 255;
            int green = (pixels[i] >> 8) & 255;
            int blue = pixels[i] & 255;
            
            int gray = (int)(0.299 * red + 0.587 * green + 0.114 * blue);
            
            pixels[i] = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
        }
        
        src.setRGB(0, 0, imWidth, imHeight, pixels, 0, imWidth);
        return src;
    }
    
    @Override
    public void update() {
        
    }
    
    @Override
    public void draw(Graphics2D g2) {
        if (!active) return;
        
        if (grayImage != null) {
            g2.drawImage(grayImage, x, y, width, height, null);
        } else if (originalImage != null) {
            g2.drawImage(originalImage, x, y, width, height, null);
        }
    }
    
}

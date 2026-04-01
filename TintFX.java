import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class TintFX implements ImageFX {
    
    private int x;
    private int y;
    private int width;
    private int height;
    private BufferedImage originalImage;
    private BufferedImage tintedImage;
    private int tintColor;  // RGB tint color
    private int tintStrength;
    private int time;
    private int timeChange;
    private boolean active;
    
    public TintFX(int xPos, int yPos, int w, int h, String imagePath, int tintRGB) {
        x = xPos;
        y = yPos;
        width = w;
        height = h;
        tintColor = tintRGB;
        tintStrength = 0;
        time = 0;
        timeChange = 1;
        active = true;
        
        originalImage = ImageManager.loadBufferedImage(imagePath);
        if (originalImage != null) {
            tintedImage = applyTint(ImageManager.copyImage(originalImage), tintColor);
        }
    }
    
    private BufferedImage applyTint(BufferedImage src, int tintRGB) {
        if (src == null) return null;
        
        int imWidth = src.getWidth();
        int imHeight = src.getHeight();
        
        int tintRed = (tintRGB >> 16) & 255;
        int tintGreen = (tintRGB >> 8) & 255;
        int tintBlue = tintRGB & 255;
        
        int[] pixels = new int[imWidth * imHeight];
        src.getRGB(0, 0, imWidth, imHeight, pixels, 0, imWidth);
        
        for (int i = 0; i < pixels.length; i++) {
            int alpha = (pixels[i] >> 24) & 255;
            int red = (pixels[i] >> 16) & 255;
            int green = (pixels[i] >> 8) & 255;
            int blue = pixels[i] & 255;
            
            // Blend with tint color
            red = (red + tintRed) / 2;
            green = (green + tintGreen) / 2;
            blue = (blue + tintBlue) / 2;
            
            pixels[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        
        src.setRGB(0, 0, imWidth, imHeight, pixels, 0, imWidth);
        return src;
    }
    
    @Override
    public void update() {
        if (!active) return;
        
        time += timeChange;
        
        // Oscillate between original and tinted
        if (time < 20) {
            tintStrength = 0;
        } else if (time < 40) {
            tintStrength = 100;
        } else {
            time = 0;
        }
    }
    
    @Override
    public void draw(Graphics2D g2) {
        if (!active) return;
        
        if (tintStrength > 0 && tintedImage != null) {
            g2.drawImage(tintedImage, x, y, width, height, null);
        } else if (originalImage != null) {
            g2.drawImage(originalImage, x, y, width, height, null);
        }
    }
    
}

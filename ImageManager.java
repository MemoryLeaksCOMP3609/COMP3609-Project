import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;


// Image loading and processing with BufferedImage support.
public class ImageManager {
    
    public ImageManager() {
    }
    
    public static Image loadImage(String fileName) {
        return new ImageIcon(fileName).getImage();
    }
    
    public static BufferedImage loadBufferedImage(String filename) {
        BufferedImage bi = null;
        
        File file = new File(filename);
        try {
            bi = ImageIO.read(file);
        } catch (IOException ioe) {
            System.out.println("Error opening file " + filename + ": " + ioe);
        }
        return bi;
    }
    
    public static BufferedImage copyImage(BufferedImage src) {
        if (src == null)
            return null;
        
        int imWidth = src.getWidth();
        int imHeight = src.getHeight();
        
        BufferedImage copy = new BufferedImage(imWidth, imHeight, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(src, 0, 0, null);
        g2d.dispose();
        
        return copy;
    }
    
    // Scale a BufferedImage to the specified width and height.
    private static BufferedImage scaleImage(BufferedImage src, int newWidth, int newHeight) {
        if (src == null) return null;
        
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.drawImage(src, 0, 0, newWidth, newHeight, null);
        g2.dispose();
        
        return scaled;
    }
    
    // Scale a BufferedImage to a specific height while maintaining aspect ratio.
    public static BufferedImage scaleImageToHeight(BufferedImage src, int targetHeight) {
        if (src == null) return null;
        
        int newWidth = (int) (src.getWidth() * ((double) targetHeight / src.getHeight()));
        return scaleImage(src, newWidth, targetHeight);
    }
    
    // Scale a BufferedImage to a specific width while maintaining aspect ratio.
    public static BufferedImage scaleImageToWidth(BufferedImage src, int targetWidth) {
        if (src == null) return null;
        
        int newHeight = (int) (src.getHeight() * ((double) targetWidth / src.getWidth()));
        return scaleImage(src, targetWidth, newHeight);
    }
    
    // Load and scale tree images to max height.
    public static BufferedImage[] loadTreeImages(int targetHeight) {
        BufferedImage[] trees = new BufferedImage[6];
        BufferedImage[] originals = new BufferedImage[6];
        originals[0] = loadBufferedImage("images/trees/Tree1.png");
        originals[1] = loadBufferedImage("images/trees/Tree2.png");
        originals[2] = loadBufferedImage("images/trees/Tree3.png");
        originals[3] = loadBufferedImage("images/trees/Tree4.png");
        originals[4] = loadBufferedImage("images/trees/Tree5.png");
        originals[5] = loadBufferedImage("images/trees/Tree6.png");
        
        for (int i = 0; i < originals.length; i++) {
            if (originals[i] != null) {
                trees[i] = scaleImageToHeight(originals[i], targetHeight);
            }
        }
        
        return trees;
    }
    
    // Load and scale rock images to max width.
    public static BufferedImage[] loadRockImages(int targetWidth) {
        BufferedImage[] rocks = new BufferedImage[9];
        BufferedImage[] originals = new BufferedImage[9];
        originals[0] = loadBufferedImage("images/rocks/Rock1.png");
        originals[1] = loadBufferedImage("images/rocks/Rock2.png");
        originals[2] = loadBufferedImage("images/rocks/Rock3.png");
        originals[3] = loadBufferedImage("images/rocks/Rock4.png");
        originals[4] = loadBufferedImage("images/rocks/Rock5.png");
        originals[5] = loadBufferedImage("images/rocks/Rock6.png");
        originals[6] = loadBufferedImage("images/rocks/Rock7.png");
        originals[7] = loadBufferedImage("images/rocks/Rock8.png");
        originals[8] = loadBufferedImage("images/rocks/Rock9.png");
        
        for (int i = 0; i < originals.length; i++) {
            if (originals[i] != null) {
                rocks[i] = scaleImageToWidth(originals[i], targetWidth);
            }
        }
        
        return rocks;
    }
    
}

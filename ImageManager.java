import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    public static BufferedImage tintVisiblePixels(BufferedImage src, Color tintColor, float blendAmount) {
        if (src == null) {
            return null;
        }

        float clampedBlend = Math.max(0.0f, Math.min(1.0f, blendAmount));
        BufferedImage tinted = copyImage(src);
        int width = tinted.getWidth();
        int height = tinted.getHeight();
        int tintRed = tintColor.getRed();
        int tintGreen = tintColor.getGreen();
        int tintBlue = tintColor.getBlue();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = tinted.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) {
                    continue;
                }

                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;

                int blendedRed = Math.round(red * (1.0f - clampedBlend) + tintRed * clampedBlend);
                int blendedGreen = Math.round(green * (1.0f - clampedBlend) + tintGreen * clampedBlend);
                int blendedBlue = Math.round(blue * (1.0f - clampedBlend) + tintBlue * clampedBlend);

                int tintedArgb = (alpha << 24) | (blendedRed << 16) | (blendedGreen << 8) | blendedBlue;
                tinted.setRGB(x, y, tintedArgb);
            }
        }

        return tinted;
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

    public static BufferedImage scaleImageByFactor(BufferedImage src, double scaleFactor) {
        if (src == null) return null;
        int newWidth = Math.max(1, (int) Math.round(src.getWidth() * scaleFactor));
        int newHeight = Math.max(1, (int) Math.round(src.getHeight() * scaleFactor));
        return scaleImage(src, newWidth, newHeight);
    }
    
    private static List<File> listPngFilesRecursively(String directoryPath) {
        List<File> pngFiles = new ArrayList<>();
        File directory = new File(directoryPath);
        collectPngFiles(directory, pngFiles);
        pngFiles.sort((left, right) -> left.getPath().compareToIgnoreCase(right.getPath()));
        return pngFiles;
    }

    public static BufferedImage[] loadBufferedImagesFromDirectory(String directoryPath) {
        List<File> imageFiles = listPngFilesRecursively(directoryPath);
        BufferedImage[] images = new BufferedImage[imageFiles.size()];

        for (int i = 0; i < imageFiles.size(); i++) {
            images[i] = loadBufferedImage(imageFiles.get(i).getPath());
        }

        return images;
    }

    private static void collectPngFiles(File file, List<File> pngFiles) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isFile()) {
            if (file.getName().toLowerCase().endsWith(".png")) {
                pngFiles.add(file);
            }
            return;
        }

        File[] children = file.listFiles();
        if (children == null) {
            return;
        }

        Arrays.sort(children, (left, right) -> left.getPath().compareToIgnoreCase(right.getPath()));
        for (File child : children) {
            collectPngFiles(child, pngFiles);
        }
    }

    // Load and scale tree images to max height.
    public static BufferedImage[] loadTreeImages(int targetHeight) {
        List<File> treeFiles = listPngFilesRecursively("images/objects/trees");
        BufferedImage[] trees = new BufferedImage[treeFiles.size()];

        for (int i = 0; i < treeFiles.size(); i++) {
            BufferedImage original = loadBufferedImage(treeFiles.get(i).getPath());
            if (original != null) {
                trees[i] = scaleImageToHeight(original, targetHeight);
            }
        }

        return trees;
    }
    
    // Load and scale rock images to max width.
    public static BufferedImage[] loadRockImages(int targetWidth) {
        List<File> rockFiles = listPngFilesRecursively("images/objects/rocks");
        BufferedImage[] rocks = new BufferedImage[rockFiles.size()];

        for (int i = 0; i < rockFiles.size(); i++) {
            BufferedImage original = loadBufferedImage(rockFiles.get(i).getPath());
            if (original != null) {
                rocks[i] = scaleImageToWidth(original, targetWidth);
            }
        }

        return rocks;
    }
    
}

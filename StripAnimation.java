import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * For extracting and managing animations from sprite sheets.
 * Provides methods to extract frames from sprite sheet rows, create animations, and flip images horizontally.
 */
public class StripAnimation {
    
    // Sprite sheet configuration constants
    public static final int FRAME_WIDTH = 90;
    public static final int FRAME_HEIGHT = 130;
    public static final int FRAMES_PER_ROW = 5;
    
    // Frame dimensions (can be customized via constructor)
    private int frameWidth;
    private int frameHeight;
    private int framesPerRow;
    
    // Animation speed control (in milliseconds per frame)
    private long animationSpeed = 200;
    
    // Default constructor using default frame dimensions.
    public StripAnimation() {
        this(FRAME_WIDTH, FRAME_HEIGHT, FRAMES_PER_ROW);
    }
    

    // Constructor with custom frame dimensions.
    public StripAnimation(int frameWidth, int frameHeight, int framesPerRow) {
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.framesPerRow = framesPerRow;
    }
    
    public void setAnimationSpeed(long speed) {
        if (speed < 1) {
            this.animationSpeed = 1;
        } else {
            this.animationSpeed = speed;
        }
    }
    
    /**
     * Extract frames from a specific row in the sprite sheet.
     * Each row represents a different animation direction.
     */
    public BufferedImage[] extractFramesFromRow(BufferedImage spriteSheet, int rowIndex) {
        if (spriteSheet == null) {
            return new BufferedImage[0];
        }
        
        BufferedImage[] frames = new BufferedImage[framesPerRow];
        int y = rowIndex * frameHeight;
        
        for (int i = 0; i < framesPerRow; i++) {
            int x = i * frameWidth;
            
            // so we don't go beyond the sprite sheet bounds
            if (x + frameWidth <= spriteSheet.getWidth() && y + frameHeight <= spriteSheet.getHeight()) {
                frames[i] = spriteSheet.getSubimage(x, y, frameWidth, frameHeight);
            } else {
                break;
            }
        }
        
        // Count valid frames
        int validFrames = 0;
        for (BufferedImage frame : frames) {
            if (frame != null) {
                validFrames++;
            }
        }
        
        // Return array with only valid frames
        BufferedImage[] result = new BufferedImage[validFrames];
        System.arraycopy(frames, 0, result, 0, validFrames);
        
        return result;
    }
    
    // Create an animation from an array of frames
    public Animation createAnimationFromFrames(BufferedImage[] frames, long frameDuration, boolean flip) {
        Animation anim = new Animation(true);
        
        for (BufferedImage frame : frames) {
            if (flip) {
                BufferedImage flippedFrame = flipImageHorizontally(frame);
                if (flippedFrame != null) {
                    anim.addFrame(flippedFrame, frameDuration);
                }
            } else {
                anim.addFrame(frame, frameDuration);
            }
        }
        
        return anim;
    }
    
    // Flip an image horizontally using AffineTransform.
    public BufferedImage flipImageHorizontally(BufferedImage src) {
        if (src == null) return null;
        
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage flipped = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2d = flipped.createGraphics();
        
        // Create affine transform for horizontal flip
        AffineTransform transform = new AffineTransform();
        transform.translate(width, 0);
        transform.scale(-1, 1);
        
        g2d.drawImage(src, transform, null);
        g2d.dispose();
        
        return flipped;
    }
    
    /**
     * Load sprite strip and create animations keyed by direction.
     * Returns a Map where keys are direction constants and values are Animation objects.
     */
    public static Map<Integer, Animation> loadSpriteAnimations(String spriteSheetPath, long frameDuration) {
        Map<Integer, Animation> animations = new HashMap<>();
        
        // Load the sprite strip using ImageManager
        BufferedImage spriteSheet = ImageManager.loadBufferedImage(spriteSheetPath);
        
        if (spriteSheet == null) {
            System.out.println("Failed to load sprite sheet: " + spriteSheetPath);
            return animations;
        }
        
        StripAnimation stripAnim = new StripAnimation();

        // New sprite sheet rows, top to bottom:
        // 0 UP, 1 UP+RIGHT, 2 RIGHT, 3 DOWN+RIGHT, 4 DOWN, 5 DOWN+LEFT, 6 LEFT, 7 UP+LEFT
        for (int rowIndex = 0; rowIndex < 8; rowIndex++) {
            BufferedImage[] frames = stripAnim.extractFramesFromRow(spriteSheet, rowIndex);
            if (frames.length > 0) {
                animations.put(rowIndex, stripAnim.createAnimationFromFrames(frames, frameDuration, false));
            }
        }
        
        return animations;
    }
    
}

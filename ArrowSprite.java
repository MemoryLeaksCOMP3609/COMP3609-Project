import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Arrow sprite, positions itself 250px from the player in the direction
 * of the nearest uncollected collectible coin, pointing towards it.
 * Uses DisappearFX for the fading effect.
 */
public class ArrowSprite {
    
    private BufferedImage originalImage;
    private BufferedImage currentImage;
    private DisappearFX disappearFX;
    private int screenX;
    private int screenY;
    private double rotationAngle;
    private float currentAlpha;
    private static final double ORBIT_DISTANCE = 250;
    private static final int MIN_FADE_DISTANCE = 300;
    
    // Arrow dimensions
    private int width;
    private int height;

    // Constructor that loads the arrow image and creates DisappearFX instance.
    public ArrowSprite() {
        originalImage = ImageManager.loadBufferedImage("images/Arrow.png");
        
        if (originalImage != null) {
            width = originalImage.getWidth();
            height = originalImage.getHeight();
            // Create a copy for pixel manipulation
            currentImage = ImageManager.copyImage(originalImage);
        } else {
            width = 32;
            height = 32;
        }
        
        disappearFX = new DisappearFX(0, 0, width, height, "images/Arrow.png");
        
        rotationAngle = 0;
        screenX = 0;
        screenY = 0;
        currentAlpha = 1.0f;
    }
    
    public void update(int playerScreenX, int playerScreenY, ArrayList<Collectible> collectibles) {
        // Find the nearest uncollected collectible relative to the player
        Collectible nearestCoin = findNearestUncollectedCoin(playerScreenX, playerScreenY, collectibles);
        
        if (nearestCoin != null) {
            // Calculate the distance from player to nearest coin
            double dx = nearestCoin.getScreenX() - playerScreenX;
            double dy = nearestCoin.getScreenY() - playerScreenY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            // Calculate alpha based on distance:
            if (distance >= 500) {
                currentAlpha = 1.0f;
            } else if (distance <= MIN_FADE_DISTANCE) {
                currentAlpha = 0.0f;
            } else {
                currentAlpha = (float)((distance - MIN_FADE_DISTANCE) / 200);
            }
            
            // Calculate the angle from player to coin (in radians)
            double angleToCoin = Math.atan2(dy, dx);
            
            // Position the arrow at 250px from player in the direction of the coin
            screenX = playerScreenX + (int)(Math.cos(angleToCoin) * ORBIT_DISTANCE);
            screenY = playerScreenY + (int)(Math.sin(angleToCoin) * ORBIT_DISTANCE);
            
            // Update DisappearFX position
            disappearFX.setPosition(screenX, screenY);
            
            // Convert to degrees for AffineTransform
            rotationAngle = Math.toDegrees(angleToCoin);
        } else {
            screenX = playerScreenX;
            screenY = playerScreenY;
            rotationAngle = 0;
            currentAlpha = 0.0f;
            disappearFX.setPosition(screenX, screenY);
        }
    }

    // Find the nearest uncollected collectible from the list relative to a position.
    private Collectible findNearestUncollectedCoin(int fromX, int fromY, ArrayList<Collectible> collectibles) {
        if (collectibles == null || collectibles.isEmpty()) {
            return null;
        }
        
        Collectible nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Collectible collectible : collectibles) {
            if (!collectible.isCollected()) {
                double dx = collectible.getScreenX() - fromX;
                double dy = collectible.getScreenY() - fromY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = collectible;
                }
            }
        }
        
        return nearest;
    }
    
    /**
     * Draw the arrow with rotation applied.
     * Uses AffineTransform for proper rotation around the arrow's center.
     * Uses AlphaComposite for proper alpha/fade effect based on distance to coin.
     */
    public void draw(Graphics2D g2) {
        // Get the alpha-applied image from DisappearFX
        BufferedImage imageToDraw = disappearFX.getCurrentImage();
        
        if (imageToDraw == null || currentAlpha <= 0.0f) {
            return;
        }
        
        // Save the current transform and composite
        AffineTransform originalTransform = g2.getTransform();
        Composite originalComposite = g2.getComposite();
        
        // Calculate center of the arrow
        int centerX = screenX + width / 2;
        int centerY = screenY + height / 2;
        
        // Create transform for rotation around center
        AffineTransform transform = new AffineTransform();
        transform.translate(centerX, centerY);
        transform.rotate(Math.toRadians(rotationAngle));
        transform.translate(-width / 2, -height / 2);
        
        // Apply the transform
        g2.setTransform(transform);
        
        // Enable alpha blending for proper transparency rendering
        // This ensures the per-pixel alpha from DisappearFX is respected
        // The normal method from the class wasn't working
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, currentAlpha));
        
        // Draw the image with alpha composite applied
        g2.drawImage(imageToDraw, 0, 0, width, height, null);
        
        // Restore the original transform and composite
        g2.setTransform(originalTransform);
        g2.setComposite(originalComposite);
    }
    
    public int getScreenX() {
        return screenX;
    }
    
    public int getScreenY() {
        return screenY;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
}

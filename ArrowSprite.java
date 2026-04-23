import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Arrow sprite that points toward the nearest uncollected collectible.
 * Its screen position is derived from the active viewport so it stays visible
 * in fullscreen instead of relying on the old fixed 800x600 layout.
 */
public class ArrowSprite {
    private static final int EDGE_PADDING = 48;
    private static final double VISIBLE_COIN_SAFE_ZONE_RATIO = 0.80;
    private static final float MIN_ALPHA = 0.0f;
    private static final float MAX_ALPHA = 1.0f;
    private final BufferedImage originalImage;
    private final BufferedImage portalImage;
    private BufferedImage currentImage;
    private DisappearFX disappearFX;
    private int screenX;
    private int screenY;
    private double rotationAngle;
    private float currentAlpha;
    private static final int MIN_FADE_DISTANCE = 50;
    
    // Arrow dimensions
    private int width;
    private int height;

    // Constructor that loads the arrow image and creates DisappearFX instance.
    public ArrowSprite() {
        originalImage = ImageManager.loadBufferedImage("images/Arrow.png");
        
        if (originalImage != null) {
            width = originalImage.getWidth();
            height = originalImage.getHeight();
            currentImage = ImageManager.copyImage(originalImage);
            portalImage = tintImage(originalImage, new Color(70, 150, 255));
        } else {
            width = 32;
            height = 32;
            currentImage = null;
            portalImage = null;
        }
        
        disappearFX = new DisappearFX(0, 0, width, height, "images/Arrow.png");
        
        rotationAngle = 0;
        screenX = 0;
        screenY = 0;
        currentAlpha = 1.0f;
    }
    
    public void update(int playerCenterX, int playerCenterY, int viewportWidth, int viewportHeight,
                       ArrayList<Collectible> collectibles, LevelPortal portal, boolean pointToPortal,
                       int cameraX, int cameraY) {
        if (pointToPortal && portal != null) {
            updateForPortal(playerCenterX, playerCenterY, viewportWidth, viewportHeight, portal, cameraX, cameraY);
            return;
        }

        useDefaultImage();

        if (hasVisibleCollectibleInSafeZone(collectibles, viewportWidth, viewportHeight)) {
            screenX = playerCenterX - width / 2;
            screenY = playerCenterY - height / 2;
            rotationAngle = 0;
            currentAlpha = 0.0f;
            disappearFX.setPosition(screenX, screenY);
            return;
        }

        // Find the nearest uncollected collectible relative to the player
        Collectible nearestCoin = findNearestUncollectedCoin(playerCenterX, playerCenterY, collectibles);
        
        if (nearestCoin != null) {
            double targetCenterX = getCollectibleCenterX(nearestCoin);
            double targetCenterY = getCollectibleCenterY(nearestCoin);
            double dx = targetCenterX - playerCenterX;
            double dy = targetCenterY - playerCenterY;
            
            // Calculate the angle from player to coin (in radians)
            double angleToCoin = Math.atan2(dy, dx);
            
            // Anchor the arrow along the direction ray while keeping it inside
            // the current viewport with a consistent edge margin.
            double placementDistance = getViewportBoundDistance(
                playerCenterX,
                playerCenterY,
                Math.cos(angleToCoin),
                Math.sin(angleToCoin),
                viewportWidth,
                viewportHeight
            );
            int arrowCenterX = playerCenterX + (int) Math.round(Math.cos(angleToCoin) * placementDistance);
            int arrowCenterY = playerCenterY + (int) Math.round(Math.sin(angleToCoin) * placementDistance);
            screenX = arrowCenterX - width / 2;
            screenY = arrowCenterY - height / 2;

            double arrowToCoinDx = targetCenterX - arrowCenterX;
            double arrowToCoinDy = targetCenterY - arrowCenterY;
            double arrowToCoinDistance = Math.sqrt(arrowToCoinDx * arrowToCoinDx + arrowToCoinDy * arrowToCoinDy);

            // Keep the same fade thresholds, but base them on the arrow's
            // current position instead of the player's screen position.
            if (arrowToCoinDistance >= 500) {
                currentAlpha = 1.0f;
            } else if (arrowToCoinDistance <= MIN_FADE_DISTANCE) {
                currentAlpha = 0.0f;
            } else {
                currentAlpha = (float) ((arrowToCoinDistance - MIN_FADE_DISTANCE) / 200);
            }
            currentAlpha = clampAlpha(currentAlpha);
            
            // Update DisappearFX position
            disappearFX.setPosition(screenX, screenY);
            
            // Convert to degrees for AffineTransform
            rotationAngle = Math.toDegrees(angleToCoin);
        } else {
            screenX = playerCenterX - width / 2;
            screenY = playerCenterY - height / 2;
            rotationAngle = 0;
            currentAlpha = MIN_ALPHA;
            disappearFX.setPosition(screenX, screenY);
        }
    }

    private void updateForPortal(int playerCenterX, int playerCenterY, int viewportWidth, int viewportHeight,
                                 LevelPortal portal, int cameraX, int cameraY) {
        usePortalImage();

        Rectangle2D.Double portalBounds = portal.getBoundingRectangle();
        double portalCenterX = portalBounds.getCenterX() - cameraX;
        double portalCenterY = portalBounds.getCenterY() - cameraY;

        if (isPointInSafeZone(portalCenterX, portalCenterY, viewportWidth, viewportHeight)) {
            screenX = playerCenterX - width / 2;
            screenY = playerCenterY - height / 2;
            rotationAngle = 0;
            currentAlpha = 0.0f;
            disappearFX.setPosition(screenX, screenY);
            return;
        }

        double dx = portalCenterX - playerCenterX;
        double dy = portalCenterY - playerCenterY;
        double angleToPortal = Math.atan2(dy, dx);
        double placementDistance = getViewportBoundDistance(
                playerCenterX,
                playerCenterY,
                Math.cos(angleToPortal),
                Math.sin(angleToPortal),
                viewportWidth,
                viewportHeight);
        int arrowCenterX = playerCenterX + (int) Math.round(Math.cos(angleToPortal) * placementDistance);
        int arrowCenterY = playerCenterY + (int) Math.round(Math.sin(angleToPortal) * placementDistance);
        screenX = arrowCenterX - width / 2;
        screenY = arrowCenterY - height / 2;

        double arrowToPortalDx = portalCenterX - arrowCenterX;
        double arrowToPortalDy = portalCenterY - arrowCenterY;
        double arrowToPortalDistance = Math.sqrt(arrowToPortalDx * arrowToPortalDx + arrowToPortalDy * arrowToPortalDy);

        if (arrowToPortalDistance >= 500) {
            currentAlpha = 1.0f;
        } else if (arrowToPortalDistance <= MIN_FADE_DISTANCE) {
            currentAlpha = 0.0f;
        } else {
            currentAlpha = (float) ((arrowToPortalDistance - MIN_FADE_DISTANCE) / 200);
        }
        currentAlpha = clampAlpha(currentAlpha);
        disappearFX.setPosition(screenX, screenY);
        rotationAngle = Math.toDegrees(angleToPortal);
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
                double dx = getCollectibleCenterX(collectible) - fromX;
                double dy = getCollectibleCenterY(collectible) - fromY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = collectible;
                }
            }
        }
        
        return nearest;
    }

    private double getCollectibleCenterX(Collectible collectible) {
        return collectible.getScreenX() + collectible.getWidth() / 2.0;
    }

    private double getCollectibleCenterY(Collectible collectible) {
        return collectible.getScreenY() + collectible.getHeight() / 2.0;
    }

    private float clampAlpha(float alpha) {
        if (Float.isNaN(alpha) || Float.isInfinite(alpha)) {
            return MIN_ALPHA;
        }
        return Math.max(MIN_ALPHA, Math.min(MAX_ALPHA, alpha));
    }

    private boolean hasVisibleCollectibleInSafeZone(ArrayList<Collectible> collectibles, int viewportWidth, int viewportHeight) {
        if (collectibles == null || collectibles.isEmpty()) {
            return false;
        }

        for (Collectible collectible : collectibles) {
            if (collectible.isCollected()) {
                continue;
            }

            double collectibleCenterX = getCollectibleCenterX(collectible);
            double collectibleCenterY = getCollectibleCenterY(collectible);
            if (isPointInSafeZone(collectibleCenterX, collectibleCenterY, viewportWidth, viewportHeight)) {
                return true;
            }
        }

        return false;
    }

    private boolean isPointInSafeZone(double pointX, double pointY, int viewportWidth, int viewportHeight) {
        double safeZoneWidth = viewportWidth * VISIBLE_COIN_SAFE_ZONE_RATIO;
        double safeZoneHeight = viewportHeight * VISIBLE_COIN_SAFE_ZONE_RATIO;
        double safeZoneMinX = (viewportWidth - safeZoneWidth) / 2.0;
        double safeZoneMaxX = safeZoneMinX + safeZoneWidth;
        double safeZoneMinY = (viewportHeight - safeZoneHeight) / 2.0;
        double safeZoneMaxY = safeZoneMinY + safeZoneHeight;
        return pointX >= safeZoneMinX && pointX <= safeZoneMaxX
                && pointY >= safeZoneMinY && pointY <= safeZoneMaxY;
    }

    private void useDefaultImage() {
        currentImage = originalImage != null ? ImageManager.copyImage(originalImage) : null;
    }

    private void usePortalImage() {
        currentImage = portalImage != null ? ImageManager.copyImage(portalImage) : null;
    }

    private BufferedImage tintImage(BufferedImage source, Color tint) {
        BufferedImage tinted = ImageManager.copyImage(source);
        int tintRed = tint.getRed();
        int tintGreen = tint.getGreen();
        int tintBlue = tint.getBlue();

        for (int y = 0; y < tinted.getHeight(); y++) {
            for (int x = 0; x < tinted.getWidth(); x++) {
                int argb = tinted.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) {
                    continue;
                }

                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;

                int tintedRed = (red * tintRed) / 255;
                int tintedGreen = (green * tintGreen) / 255;
                int tintedBlue = (blue * tintBlue) / 255;
                int tintedArgb = (alpha << 24) | (tintedRed << 16) | (tintedGreen << 8) | tintedBlue;
                tinted.setRGB(x, y, tintedArgb);
            }
        }

        return tinted;
    }

    private double getViewportBoundDistance(int playerCenterX, int playerCenterY, double directionX, double directionY,
                                            int viewportWidth, int viewportHeight) {
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        double minX = EDGE_PADDING + halfWidth;
        double maxX = Math.max(minX, viewportWidth - EDGE_PADDING - halfWidth);
        double minY = EDGE_PADDING + halfHeight;
        double maxY = Math.max(minY, viewportHeight - EDGE_PADDING - halfHeight);
        double maxDistance = Double.POSITIVE_INFINITY;

        if (directionX > 0.0001) {
            maxDistance = Math.min(maxDistance, (maxX - playerCenterX) / directionX);
        } else if (directionX < -0.0001) {
            maxDistance = Math.min(maxDistance, (minX - playerCenterX) / directionX);
        }

        if (directionY > 0.0001) {
            maxDistance = Math.min(maxDistance, (maxY - playerCenterY) / directionY);
        } else if (directionY < -0.0001) {
            maxDistance = Math.min(maxDistance, (minY - playerCenterY) / directionY);
        }

        if (!Double.isFinite(maxDistance) || maxDistance < 0) {
            return 0;
        }

        return maxDistance;
    }
    
    /**
     * Draw the arrow with rotation applied.
     * Uses AffineTransform for proper rotation around the arrow's center.
     * Uses AlphaComposite for proper alpha/fade effect based on distance to coin.
     */
    public void draw(Graphics2D g2) {
        BufferedImage imageToDraw = currentImage;
        
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

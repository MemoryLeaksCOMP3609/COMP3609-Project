import java.awt.geom.AffineTransform;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class Projectile {
    private static final double MOVEMENT_REFERENCE_FRAME_MS = 40.0;
    private static final long DEFAULT_FRAME_DURATION_MS = 50;
    private static final Map<String, BufferedImage[]> FRAME_CACHE = new HashMap<String, BufferedImage[]>();

    private double worldX;
    private double worldY;
    private final double velocityX;
    private final double velocityY;
    private final int damage;
    private final boolean enemyOwned;
    private final double renderScale;
    private final BufferedImage[] frames;
    private final boolean baseImageFacesLeft;
    private final double rotationRadians;
    private long animationElapsedMs;
    private int currentFrameIndex;
    private boolean active;

    public Projectile(double worldX, double worldY, double velocityX, double velocityY,
                      int damage, boolean enemyOwned, String frameDirectory,
                      double renderScale, boolean baseImageFacesLeft, double rotationRadians) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.damage = damage;
        this.enemyOwned = enemyOwned;
        this.renderScale = renderScale;
        this.frames = loadFrames(frameDirectory);
        this.baseImageFacesLeft = baseImageFacesLeft;
        this.rotationRadians = rotationRadians;
        this.animationElapsedMs = 0;
        this.currentFrameIndex = 0;
        this.active = true;
    }

    public void update(long deltaTimeMs) {
        double distanceScale = deltaTimeMs / MOVEMENT_REFERENCE_FRAME_MS;
        worldX += velocityX * distanceScale;
        worldY += velocityY * distanceScale;
        if (frames.length > 1) {
            animationElapsedMs += deltaTimeMs;
            currentFrameIndex = (int) ((animationElapsedMs / DEFAULT_FRAME_DURATION_MS) % frames.length);
        }
    }

    public void draw(Graphics2D g2, int cameraX, int cameraY) {
        if (!active || frames.length == 0) {
            return;
        }

        BufferedImage currentFrame = frames[currentFrameIndex];
        int drawWidth = Math.max(1, (int) Math.round(currentFrame.getWidth() * renderScale));
        int drawHeight = Math.max(1, (int) Math.round(currentFrame.getHeight() * renderScale));
        int drawX = (int) Math.round(worldX) - cameraX - drawWidth / 2;
        int drawY = (int) Math.round(worldY) - cameraY - drawHeight / 2;

        AffineTransform originalTransform = g2.getTransform();
        AffineTransform transform = new AffineTransform();
        transform.translate(drawX + drawWidth / 2.0, drawY + drawHeight / 2.0);
        double adjustedRotation = baseImageFacesLeft ? rotationRadians + Math.PI : rotationRadians;
        transform.rotate(adjustedRotation);
        transform.translate(-drawWidth / 2.0, -drawHeight / 2.0);
        g2.setTransform(transform);
        g2.drawImage(currentFrame, 0, 0, drawWidth, drawHeight, null);
        g2.setTransform(originalTransform);
    }

    public Rectangle2D.Double getBounds() {
        if (frames.length == 0) {
            return new Rectangle2D.Double(worldX, worldY, 1, 1);
        }

        BufferedImage currentFrame = frames[currentFrameIndex];
        double drawWidth = Math.max(1, Math.round(currentFrame.getWidth() * renderScale));
        double drawHeight = Math.max(1, Math.round(currentFrame.getHeight() * renderScale));
        return new Rectangle2D.Double(worldX - drawWidth / 2.0, worldY - drawHeight / 2.0, drawWidth, drawHeight);
    }

    public boolean isOutOfBounds(int worldWidth, int worldHeight) {
        double halfWidth = getBounds().getWidth() / 2.0;
        double halfHeight = getBounds().getHeight() / 2.0;
        return worldX < -halfWidth || worldY < -halfHeight || worldX > worldWidth + halfWidth || worldY > worldHeight + halfHeight;
    }

    public int getDamage() {
        return damage;
    }

    public boolean isEnemyOwned() {
        return enemyOwned;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }

    private static BufferedImage[] loadFrames(String frameDirectory) {
        BufferedImage[] cachedFrames = FRAME_CACHE.get(frameDirectory);
        if (cachedFrames != null) {
            return cachedFrames;
        }

        BufferedImage[] loadedFrames = ImageManager.loadBufferedImagesFromDirectory(frameDirectory);
        FRAME_CACHE.put(frameDirectory, loadedFrames);
        return loadedFrames;
    }
}

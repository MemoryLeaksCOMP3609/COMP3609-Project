import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class Projectile {
    public enum MotionMode {
        STRAIGHT,
        HOMING,
        ORBIT
    }

    private static final double MOVEMENT_REFERENCE_FRAME_MS = 40.0;
    private static final long DEFAULT_FRAME_DURATION_MS = 50;
    private static final long IMPACT_LINGER_MS = 120;
    private static final Map<String, BufferedImage[]> FRAME_CACHE = new HashMap<String, BufferedImage[]>();

    private double worldX;
    private double worldY;
    private double velocityX;
    private double velocityY;
    private final int damage;
    private final boolean enemyOwned;
    private final double renderScale;
    private final BufferedImage[] frames;
    private final boolean baseImageFacesLeft;
    private double rotationRadians;
    private final double hitboxLengthScale;
    private final double hitboxThicknessScale;
    private final WeaponType weaponType;
    private final MotionMode motionMode;

    private double homingTurnRateRadians;
    private double orbitRadius;
    private double orbitAngle;
    private double orbitAngularSpeed;
    private long contactCooldownMs;
    private long impactLingerRemainingMs;
    private long animationElapsedMs;
    private int currentFrameIndex;
    private boolean active;
    private boolean impacted;

    public Projectile(double worldX, double worldY, double velocityX, double velocityY,
                      int damage, boolean enemyOwned, String frameDirectory,
                      double renderScale, boolean baseImageFacesLeft, double rotationRadians,
                      double hitboxLengthScale, double hitboxThicknessScale,
                      WeaponType weaponType, MotionMode motionMode) {
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
        this.hitboxLengthScale = hitboxLengthScale;
        this.hitboxThicknessScale = hitboxThicknessScale;
        this.weaponType = weaponType;
        this.motionMode = motionMode;
        this.homingTurnRateRadians = 0.10;
        this.orbitRadius = 120.0;
        this.orbitAngle = rotationRadians;
        this.orbitAngularSpeed = 0.0;
        this.contactCooldownMs = 0;
        this.impactLingerRemainingMs = 0;
        this.animationElapsedMs = 0;
        this.currentFrameIndex = 0;
        this.active = true;
        this.impacted = false;
    }

    public void update(long deltaTimeMs, GameWorld world, PlayerSprite player) {
        contactCooldownMs = Math.max(0, contactCooldownMs - deltaTimeMs);

        if (impacted) {
            impactLingerRemainingMs = Math.max(0, impactLingerRemainingMs - deltaTimeMs);
            if (impactLingerRemainingMs == 0) {
                active = false;
            }
            updateAnimation(deltaTimeMs);
            return;
        }

        switch (motionMode) {
            case HOMING:
                updateHoming(deltaTimeMs, world);
                break;
            case ORBIT:
                updateOrbit(deltaTimeMs, player);
                break;
            case STRAIGHT:
            default:
                updateStraight(deltaTimeMs);
                break;
        }

        updateAnimation(deltaTimeMs);
    }

    private void updateStraight(long deltaTimeMs) {
        double distanceScale = deltaTimeMs / MOVEMENT_REFERENCE_FRAME_MS;
        worldX += velocityX * distanceScale;
        worldY += velocityY * distanceScale;
    }

    private void updateHoming(long deltaTimeMs, GameWorld world) {
        Enemy nearestEnemy = findNearestEnemy(world);
        if (nearestEnemy != null) {
            double targetAngle = Math.atan2(nearestEnemy.getCenterY() - worldY, nearestEnemy.getCenterX() - worldX);
            double currentAngle = Math.atan2(velocityY, velocityX);
            double deltaAngle = normalizeAngle(targetAngle - currentAngle);
            double angleStep = homingTurnRateRadians * (deltaTimeMs / MOVEMENT_REFERENCE_FRAME_MS);
            double appliedTurn = Math.max(-angleStep, Math.min(angleStep, deltaAngle));
            currentAngle += appliedTurn;

            double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
            velocityX = Math.cos(currentAngle) * speed;
            velocityY = Math.sin(currentAngle) * speed;
            rotationRadians = currentAngle;
        }

        updateStraight(deltaTimeMs);
    }

    private void updateOrbit(long deltaTimeMs, PlayerSprite player) {
        if (player == null) {
            return;
        }

        orbitAngle += orbitAngularSpeed * deltaTimeMs;
        worldX = player.getCenterX() + Math.cos(orbitAngle) * orbitRadius;
        worldY = player.getCenterY() + Math.sin(orbitAngle) * orbitRadius;
        rotationRadians = orbitAngle + Math.PI / 2.0;
    }

    private void updateAnimation(long deltaTimeMs) {
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
        Rectangle2D bounds = getHitboxShape().getBounds2D();
        return new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    public boolean intersects(Rectangle2D targetBounds) {
        Area projectileArea = new Area(getHitboxShape());
        projectileArea.intersect(new Area(targetBounds));
        return !projectileArea.isEmpty();
    }

    private Shape getHitboxShape() {
        if (frames.length == 0) {
            return new Rectangle2D.Double(worldX, worldY, 1, 1);
        }

        BufferedImage currentFrame = frames[currentFrameIndex];
        double drawWidth = Math.max(1, Math.round(currentFrame.getWidth() * renderScale));
        double drawHeight = Math.max(1, Math.round(currentFrame.getHeight() * renderScale));
        double hitboxWidth = Math.max(4.0, drawWidth * hitboxLengthScale);
        double hitboxHeight = Math.max(4.0, drawHeight * hitboxThicknessScale);

        Rectangle2D.Double baseHitbox = new Rectangle2D.Double(
            -hitboxWidth / 2.0,
            -hitboxHeight / 2.0,
            hitboxWidth,
            hitboxHeight
        );

        AffineTransform transform = new AffineTransform();
        transform.translate(worldX, worldY);
        double adjustedRotation = baseImageFacesLeft ? rotationRadians + Math.PI : rotationRadians;
        transform.rotate(adjustedRotation);
        return transform.createTransformedShape(baseHitbox);
    }

    public boolean isOutOfBounds(int worldWidth, int worldHeight) {
        if (motionMode == MotionMode.ORBIT) {
            return false;
        }

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

    public void markImpact() {
        if (motionMode == MotionMode.ORBIT) {
            contactCooldownMs = 250;
            return;
        }

        impacted = true;
        impactLingerRemainingMs = IMPACT_LINGER_MS;
    }

    public boolean hasImpacted() {
        return impacted;
    }

    public boolean canDamage() {
        return contactCooldownMs <= 0 && !impacted;
    }

    public WeaponType getWeaponType() {
        return weaponType;
    }

    public MotionMode getMotionMode() {
        return motionMode;
    }

    public void configureHomingTurnRate(double homingTurnRateRadians) {
        this.homingTurnRateRadians = homingTurnRateRadians;
    }

    public void configureOrbit(double orbitRadius, double orbitAngle, double orbitAngularSpeed) {
        this.orbitRadius = orbitRadius;
        this.orbitAngle = orbitAngle;
        this.orbitAngularSpeed = orbitAngularSpeed;
    }

    private Enemy findNearestEnemy(GameWorld world) {
        Enemy nearestEnemy = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Enemy enemy : world.getEnemies()) {
            if (enemy.isDead()) {
                continue;
            }

            double deltaX = enemy.getCenterX() - worldX;
            double deltaY = enemy.getCenterY() - worldY;
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestEnemy = enemy;
            }
        }

        return nearestEnemy;
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI) {
            angle -= Math.PI * 2.0;
        }
        while (angle < -Math.PI) {
            angle += Math.PI * 2.0;
        }
        return angle;
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

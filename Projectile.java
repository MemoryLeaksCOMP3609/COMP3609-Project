import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public abstract class Projectile {
    public enum MotionMode {
        STRAIGHT,
        HOMING,
        ORBIT
    }

    private static final long DEFAULT_FRAME_DURATION_MS = 50L;
    private static final long IMPACT_LINGER_MS = 120L;
    private static final long DEFAULT_MAX_LIFETIME_MS = 5000L;
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

    private long contactCooldownMs;
    private long impactLingerRemainingMs;
    private long lifetimeRemainingMs;
    private long animationElapsedMs;
    private int currentFrameIndex;
    private boolean active;
    private boolean impacted;

    protected Projectile(double worldX, double worldY, double velocityX, double velocityY,
                         int damage, boolean enemyOwned, String frameDirectory,
                         double renderScale, boolean baseImageFacesLeft, double rotationRadians,
                         double hitboxLengthScale, double hitboxThicknessScale,
                         WeaponType weaponType, MotionMode motionMode, long lifetimeRemainingMs) {
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
        this.contactCooldownMs = 0L;
        this.impactLingerRemainingMs = 0L;
        this.lifetimeRemainingMs = lifetimeRemainingMs;
        this.animationElapsedMs = 0L;
        this.currentFrameIndex = 0;
        this.active = true;
        this.impacted = false;
    }

    public static Projectile create(double worldX, double worldY, double velocityX, double velocityY,
                                    int damage, boolean enemyOwned, String frameDirectory,
                                    double renderScale, boolean baseImageFacesLeft, double rotationRadians,
                                    double hitboxLengthScale, double hitboxThicknessScale,
                                    WeaponType weaponType, MotionMode motionMode) {
        switch (motionMode) {
            case HOMING:
                return new HomingProjectile(worldX, worldY, velocityX, velocityY, damage, enemyOwned,
                    frameDirectory, renderScale, baseImageFacesLeft, rotationRadians,
                    hitboxLengthScale, hitboxThicknessScale, weaponType);
            case ORBIT:
                return new OrbitProjectile(worldX, worldY, velocityX, velocityY, damage, enemyOwned,
                    frameDirectory, renderScale, baseImageFacesLeft, rotationRadians,
                    hitboxLengthScale, hitboxThicknessScale, weaponType);
            case STRAIGHT:
            default:
                return new StraightProjectile(worldX, worldY, velocityX, velocityY, damage, enemyOwned,
                    frameDirectory, renderScale, baseImageFacesLeft, rotationRadians,
                    hitboxLengthScale, hitboxThicknessScale, weaponType);
        }
    }

    protected static long getDefaultMaxLifetimeMs() {
        return DEFAULT_MAX_LIFETIME_MS;
    }

    public void update(long deltaTimeMs, GameWorld world, PlayerSprite player) {
        if (!active) {
            return;
        }

        contactCooldownMs = Math.max(0L, contactCooldownMs - deltaTimeMs);

        if (lifetimeRemainingMs != Long.MAX_VALUE) {
            lifetimeRemainingMs = Math.max(0L, lifetimeRemainingMs - deltaTimeMs);
            if (lifetimeRemainingMs == 0L) {
                active = false;
                return;
            }
        }

        if (impacted) {
            impactLingerRemainingMs = Math.max(0L, impactLingerRemainingMs - deltaTimeMs);
            if (impactLingerRemainingMs == 0L) {
                active = false;
            }
            updateAnimation(deltaTimeMs);
            return;
        }

        updateMotion(deltaTimeMs, world, player);
        updateAnimation(deltaTimeMs);
    }

    protected abstract void updateMotion(long deltaTimeMs, GameWorld world, PlayerSprite player);

    protected void updateAnimation(long deltaTimeMs) {
        if (frames.length > 1) {
            animationElapsedMs += deltaTimeMs;
            currentFrameIndex = (int) ((animationElapsedMs / DEFAULT_FRAME_DURATION_MS) % frames.length);
        }
    }

    protected void moveStraight(long deltaTimeMs, double movementReferenceFrameMs) {
        double distanceScale = deltaTimeMs / movementReferenceFrameMs;
        worldX += velocityX * distanceScale;
        worldY += velocityY * distanceScale;
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
        transform.rotate(getAdjustedRotation());
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
        transform.rotate(getAdjustedRotation());
        return transform.createTransformedShape(baseHitbox);
    }

    public boolean isOutOfBounds(int worldWidth, int worldHeight) {
        if (ignoresWorldBounds()) {
            return false;
        }

        double halfWidth = getBounds().getWidth() / 2.0;
        double halfHeight = getBounds().getHeight() / 2.0;
        return worldX < -halfWidth || worldY < -halfHeight || worldX > worldWidth + halfWidth || worldY > worldHeight + halfHeight;
    }

    protected boolean ignoresWorldBounds() {
        return false;
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
        if (usesContactCooldownOnImpact()) {
            contactCooldownMs = getImpactCooldownMs();
            return;
        }

        impacted = true;
        impactLingerRemainingMs = IMPACT_LINGER_MS;
    }

    protected boolean usesContactCooldownOnImpact() {
        return false;
    }

    protected long getImpactCooldownMs() {
        return 0L;
    }

    public boolean hasImpacted() {
        return impacted;
    }

    public boolean canDamage() {
        return contactCooldownMs <= 0L && !impacted;
    }

    public WeaponType getWeaponType() {
        return weaponType;
    }

    public MotionMode getMotionMode() {
        return motionMode;
    }

    public void configureHomingTurnRate(double homingTurnRateRadians) {
        // Default projectiles do not use homing turn rate.
    }

    public void configureOrbit(double orbitRadius, double orbitAngle, double orbitAngularSpeed) {
        // Default projectiles do not use orbit configuration.
    }

    protected Enemy findNearestEnemy(GameWorld world) {
        Enemy nearestEnemy = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Enemy enemy : world.getEnemies()) {
            if (!enemy.isTargetable()) {
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

    protected double normalizeAngle(double angle) {
        while (angle > Math.PI) {
            angle -= Math.PI * 2.0;
        }
        while (angle < -Math.PI) {
            angle += Math.PI * 2.0;
        }
        return angle;
    }

    protected double getWorldX() {
        return worldX;
    }

    protected void setWorldX(double worldX) {
        this.worldX = worldX;
    }

    protected double getWorldY() {
        return worldY;
    }

    protected void setWorldY(double worldY) {
        this.worldY = worldY;
    }

    protected double getVelocityX() {
        return velocityX;
    }

    protected void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }

    protected double getVelocityY() {
        return velocityY;
    }

    protected void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    protected double getRotationRadians() {
        return rotationRadians;
    }

    protected void setRotationRadians(double rotationRadians) {
        this.rotationRadians = rotationRadians;
    }

    private double getAdjustedRotation() {
        return baseImageFacesLeft ? rotationRadians + Math.PI : rotationRadians;
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

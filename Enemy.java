import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public abstract class Enemy extends Sprite {
    private static final int DEFAULT_FRAME_COUNT = 4;
    private static final double MOVEMENT_REFERENCE_FRAME_MS = 40.0;
    private static final long DAMAGE_FLASH_DURATION_MS = 120;

    protected final String name;
    protected int maxHealth;
    protected int currentHealth;
    protected int movementSpeed;
    protected int contactDamage;
    protected int scoreValue;
    protected int experienceReward;

    protected int worldX;
    protected int worldY;
    protected int screenX;
    protected int screenY;

    protected Animation idleAnimation;
    protected Animation moveAnimation;
    protected Animation attackAnimation;
    protected Animation deathAnimation;
    protected Animation currentAnimation;
    protected EnemyState state;
    protected long attackCooldownMs;
    protected double renderScale;
    protected long damageFlashRemainingMs;
    protected boolean facingLeft;
    protected boolean spriteFacesLeftByDefault;
    protected BufferedImage cachedBaseFrame;
    protected BufferedImage cachedDamageFlashFrame;
    protected boolean defeatRewardGranted;

    protected Enemy(String name, int maxHealth, int movementSpeed, int contactDamage,
                    int scoreValue, int experienceReward, int startX, int startY) {
        super(null, startX, startY, 0, 0);
        this.name = name;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.movementSpeed = movementSpeed;
        this.contactDamage = contactDamage;
        this.scoreValue = scoreValue;
        this.experienceReward = experienceReward;
        this.worldX = startX;
        this.worldY = startY;
        this.screenX = startX;
        this.screenY = startY;
        this.state = EnemyState.IDLE;
        this.attackCooldownMs = 0;
        this.renderScale = 1.0;
        this.damageFlashRemainingMs = 0;
        this.facingLeft = true;
        this.spriteFacesLeftByDefault = true;
        this.cachedBaseFrame = null;
        this.cachedDamageFlashFrame = null;
        this.defeatRewardGranted = false;
    }

    protected Animation loadStripAnimation(String imagePath, long frameDuration, boolean loop) {
        return loadStripAnimation(imagePath, frameDuration, loop, DEFAULT_FRAME_COUNT);
    }

    protected Animation loadStripAnimation(String imagePath, long frameDuration, boolean loop, int frameCount) {
        BufferedImage[] frames = loadStripFrames(imagePath, frameCount);
        if (frames.length == 0) {
            return null;
        }
        Animation animation = buildAnimation(frames, frameDuration, loop);
        if (frames.length > 0) {
            width = frames[0].getWidth();
            height = frames[0].getHeight();
            image = frames[0];
        }
        return animation;
    }

    protected BufferedImage[] loadStripFrames(String imagePath, int frameCount) {
        BufferedImage spriteSheet = ImageManager.loadBufferedImage(imagePath);
        if (spriteSheet == null) {
            return new BufferedImage[0];
        }

        int frameWidth = spriteSheet.getWidth() / frameCount;
        int frameHeight = spriteSheet.getHeight();
        StripAnimation stripAnimation = new StripAnimation(frameWidth, frameHeight, frameCount);
        return stripAnimation.extractFramesFromRow(spriteSheet, 0);
    }

    protected Animation buildAnimation(BufferedImage[] frames, long frameDuration, boolean loop) {
        Animation animation = new Animation(loop);
        for (BufferedImage frame : frames) {
            animation.addFrame(frame, frameDuration);
        }
        return animation;
    }

    protected Animation buildAnimation(BufferedImage[] frames, long[] frameDurations, boolean loop) {
        Animation animation = new Animation(loop);
        int frameCount = Math.min(frames.length, frameDurations.length);
        for (int i = 0; i < frameCount; i++) {
            animation.addFrame(frames[i], frameDurations[i]);
        }
        return animation;
    }

    protected void setAnimationForState(EnemyState nextState) {
        state = nextState;
        switch (nextState) {
            case MOVING:
                currentAnimation = moveAnimation != null ? moveAnimation : idleAnimation;
                break;
            case ATTACKING:
                currentAnimation = attackAnimation != null ? attackAnimation : moveAnimation;
                break;
            case DYING:
            case DEAD:
                currentAnimation = deathAnimation != null ? deathAnimation : idleAnimation;
                break;
            case IDLE:
            default:
                currentAnimation = idleAnimation != null ? idleAnimation : moveAnimation;
                break;
        }

        if (currentAnimation != null && !currentAnimation.isActive()) {
            currentAnimation.start();
        }
    }

    public void update(long deltaTimeMs) {
        if (isDying()) {
            updateDeath(deltaTimeMs);
            return;
        }

        if (currentAnimation != null) {
            currentAnimation.update(deltaTimeMs);
            syncDimensionsWithCurrentFrame();
        }
    }

    public void update() {
        update(0);
    }

    public void updateScreenPosition(int cameraX, int cameraY) {
        screenX = worldX - cameraX;
        screenY = worldY - cameraY;
    }

    public void moveToward(int targetX, int targetY, long deltaTimeMs) {
        moveToward(targetX, targetY, 0, deltaTimeMs);
    }

    public void moveToward(int targetX, int targetY, int stopDistance, long deltaTimeMs) {
        if (!isAlive()) {
            return;
        }

        double deltaX = targetX - getCenterX();
        double deltaY = targetY - getCenterY();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (distance <= stopDistance || distance == 0) {
            setAnimationForState(EnemyState.IDLE);
            return;
        }

        double moveDistance = movementSpeed * (deltaTimeMs / MOVEMENT_REFERENCE_FRAME_MS);
        double directionX = deltaX / distance;
        double directionY = deltaY / distance;

        updateFacingDirection(directionX);

        worldX += (int) Math.round(directionX * moveDistance);
        worldY += (int) Math.round(directionY * moveDistance);

        setAnimationForState(EnemyState.MOVING);
    }

    protected void faceToward(int targetX) {
        updateFacingDirection(targetX - getCenterX());
    }

    protected void updateFacingDirection(double directionX) {
        if (Math.abs(directionX) > 0.001) {
            facingLeft = directionX < 0;
        }
    }

    public void attack() {
        if (isAlive()) {
            setAnimationForState(EnemyState.ATTACKING);
        }
    }

    public void takeDamage(int damage) {
        if (damage <= 0 || !canTakeDamage()) {
            return;
        }

        currentHealth = Math.max(0, currentHealth - damage);
        triggerDamageFlash();
        if (currentHealth == 0) {
            die();
        }
    }

    public void die() {
        if (isDying() || isDead()) {
            return;
        }

        setAnimationForState(EnemyState.DYING);
        onDeathStarted();
    }

    public boolean isDead() {
        return state == EnemyState.DEAD;
    }

    public boolean isDying() {
        return state == EnemyState.DYING;
    }

    public boolean isDefeated() {
        return currentHealth <= 0 || isDying() || isDead();
    }

    public boolean isAlive() {
        return !isDefeated();
    }

    public boolean isTargetable() {
        return isAlive();
    }

    public boolean canTakeDamage() {
        return isAlive();
    }

    public boolean shouldRemove() {
        return state == EnemyState.DEAD;
    }

    public boolean consumeDefeatReward() {
        if (currentHealth > 0 || !isDead() || defeatRewardGranted) {
            return false;
        }

        defeatRewardGranted = true;
        return true;
    }

    @Override
    public void draw(Graphics2D g2) {
        BufferedImage frameToDraw = getFrameToDraw();
        if (frameToDraw != null) {
            drawFrame(g2, frameToDraw, 1.0f);
        }
    }

    @Override
    public Rectangle2D.Double getBoundingRectangle() {
        syncDimensionsWithCurrentFrame();
        return new Rectangle2D.Double(worldX, worldY, width, height);
    }

    public BufferedImage getCurrentBufferedImage() {
        if (currentAnimation != null && currentAnimation.getImage() != null) {
            return PixelCollision.toBufferedImage(currentAnimation.getImage());
        }
        return PixelCollision.toBufferedImage(image);
    }

    protected void syncDimensionsWithCurrentFrame() {
        BufferedImage currentFrame = getCurrentBufferedImage();
        if (currentFrame != null) {
            width = Math.max(1, (int) Math.round(currentFrame.getWidth() * renderScale));
            height = Math.max(1, (int) Math.round(currentFrame.getHeight() * renderScale));
        }
    }

    public String getName() {
        return name;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getMovementSpeed() {
        return movementSpeed;
    }

    public int getContactDamage() {
        return contactDamage;
    }

    public int getScoreValue() {
        return scoreValue;
    }

    public int getExperienceReward() {
        return experienceReward;
    }

    public EnemyState getState() {
        return state;
    }

    public int getWorldX() {
        return worldX;
    }

    public int getWorldY() {
        return worldY;
    }

    public int getCenterX() {
        return worldX + width / 2;
    }

    public int getCenterY() {
        return worldY + height / 2;
    }

    public void updateAttackCooldown(long deltaTimeMs) {
        attackCooldownMs = Math.max(0, attackCooldownMs - deltaTimeMs);
    }

    public boolean canAttack() {
        return attackCooldownMs <= 0;
    }

    public void setAttackCooldown(long attackCooldownMs) {
        this.attackCooldownMs = attackCooldownMs;
    }

    public void updateDamageFlash(long deltaTimeMs) {
        damageFlashRemainingMs = Math.max(0, damageFlashRemainingMs - deltaTimeMs);
        if (damageFlashRemainingMs == 0) {
            cachedBaseFrame = null;
            cachedDamageFlashFrame = null;
        }
    }

    protected void triggerDamageFlash() {
        damageFlashRemainingMs = DAMAGE_FLASH_DURATION_MS;
    }

    protected BufferedImage getDamageFlashFrame(BufferedImage currentFrame) {
        if (currentFrame == null) {
            return null;
        }

        if (currentFrame != cachedBaseFrame || cachedDamageFlashFrame == null) {
            cachedBaseFrame = currentFrame;
            cachedDamageFlashFrame = ImageManager.tintVisiblePixels(currentFrame, Color.RED, 0.45f);
        }

        return cachedDamageFlashFrame;
    }

    protected BufferedImage getFrameToDraw() {
        BufferedImage currentFrame = getCurrentBufferedImage();
        if (currentFrame == null) {
            return null;
        }

        if (damageFlashRemainingMs > 0) {
            return getDamageFlashFrame(currentFrame);
        }

        return currentFrame;
    }

    protected void drawFrame(Graphics2D g2, BufferedImage frameToDraw, float alpha) {
        if (frameToDraw == null || alpha <= 0.0f) {
            return;
        }

        AffineTransform originalTransform = g2.getTransform();
        java.awt.Composite originalComposite = g2.getComposite();

        if (alpha < 1.0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        if (facingLeft == spriteFacesLeftByDefault) {
            g2.drawImage(frameToDraw, screenX, screenY, width, height, null);
        } else {
            AffineTransform flippedTransform = new AffineTransform();
            flippedTransform.translate(screenX + width, screenY);
            flippedTransform.scale(-1, 1);
            g2.transform(flippedTransform);
            g2.drawImage(frameToDraw, 0, 0, width, height, null);
        }

        g2.setTransform(originalTransform);
        g2.setComposite(originalComposite);
    }

    protected void onDeathStarted() {
        // Default death behavior only plays the death animation in place.
    }

    protected void updateDeath(long deltaTimeMs) {
        if (currentAnimation != null) {
            currentAnimation.update(deltaTimeMs);
            syncDimensionsWithCurrentFrame();
            if (!currentAnimation.isActive()) {
                state = EnemyState.DEAD;
            }
            return;
        }

        state = EnemyState.DEAD;
    }
}

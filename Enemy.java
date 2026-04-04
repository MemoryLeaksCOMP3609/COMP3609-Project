import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public abstract class Enemy extends Sprite {
    private static final int DEFAULT_FRAME_COUNT = 5;

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
    }

    protected Animation loadStripAnimation(String imagePath, long frameDuration, boolean loop) {
        BufferedImage spriteSheet = ImageManager.loadBufferedImage(imagePath);
        if (spriteSheet == null) {
            return null;
        }

        int frameWidth = spriteSheet.getWidth() / DEFAULT_FRAME_COUNT;
        int frameHeight = spriteSheet.getHeight();
        StripAnimation stripAnimation = new StripAnimation(frameWidth, frameHeight, DEFAULT_FRAME_COUNT);
        BufferedImage[] frames = stripAnimation.extractFramesFromRow(spriteSheet, 0);
        Animation animation = new Animation(loop);
        for (BufferedImage frame : frames) {
            animation.addFrame(frame, frameDuration);
        }
        if (frames.length > 0) {
            width = frames[0].getWidth();
            height = frames[0].getHeight();
            image = frames[0];
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

    public void update() {
        if (currentAnimation != null) {
            currentAnimation.update();
            syncDimensionsWithCurrentFrame();
        }
    }

    public void updateScreenPosition(int cameraX, int cameraY) {
        screenX = worldX - cameraX;
        screenY = worldY - cameraY;
    }

    public void moveToward(int targetX, int targetY) {
        if (isDead()) {
            return;
        }

        int deltaX = targetX - worldX;
        int deltaY = targetY - worldY;

        if (deltaX == 0 && deltaY == 0) {
            setAnimationForState(EnemyState.IDLE);
            return;
        }

        if (deltaX > 0) {
            worldX += movementSpeed;
        } else if (deltaX < 0) {
            worldX -= movementSpeed;
        }

        if (deltaY > 0) {
            worldY += movementSpeed;
        } else if (deltaY < 0) {
            worldY -= movementSpeed;
        }

        setAnimationForState(EnemyState.MOVING);
    }

    public void attack() {
        if (!isDead()) {
            setAnimationForState(EnemyState.ATTACKING);
        }
    }

    public void takeDamage(int damage) {
        if (damage <= 0 || isDead()) {
            return;
        }

        currentHealth = Math.max(0, currentHealth - damage);
        if (currentHealth == 0) {
            die();
        }
    }

    public void die() {
        setAnimationForState(EnemyState.DYING);
        state = EnemyState.DEAD;
    }

    public boolean isDead() {
        return state == EnemyState.DEAD || currentHealth <= 0;
    }

    @Override
    public void draw(Graphics2D g2) {
        BufferedImage currentFrame = getCurrentBufferedImage();
        if (currentFrame != null) {
            g2.drawImage(currentFrame, screenX, screenY, width, height, null);
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
            width = currentFrame.getWidth();
            height = currentFrame.getHeight();
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
}

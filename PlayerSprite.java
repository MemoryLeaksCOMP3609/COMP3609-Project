import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.swing.JPanel;

/**
 * Player sprite with keyboard movement (arrow keys/WASD) and animation states
 */
public class PlayerSprite extends Sprite {
    private static final double MOVEMENT_REFERENCE_FRAME_MS = 40.0;
    private final Player playerData;
    
    private int worldX;
    private int worldY;
    private double preciseWorldX;
    private double preciseWorldY;
    private boolean speedBoostActive = false;
    private long speedBoostTimer = 0; // Time remaining in milliseconds
    private static final long SPEED_BOOST_DURATION = 1000; // 1 second
    private static final int SPEED_BOOST_MULTIPLIER = 3; // 3x speed
    private static final long RUN_FRAME_DURATION = 28;
    private static final long DAMAGE_FLASH_DURATION_MS = 120;
    private int screenX;
    private int screenY;
    private double preciseScreenX;
    private double preciseScreenY;
    private long damageFlashRemainingMs;
    
    // Animation states
    public static final int STATE_IDLE = 0;
    public static final int STATE_RUN = 1;
    private int currentState;
    
    // Movement direction
    public static final int DIR_LEFT = 1;
    public static final int DIR_RIGHT = 2;
    public static final int DIR_UP = 3;
    public static final int DIR_DOWN = 4;
    public static final int DIR_UP_LEFT = 5;
    public static final int DIR_UP_RIGHT = 6;
    public static final int DIR_DOWN_LEFT = 7;
    public static final int DIR_DOWN_RIGHT = 8;
    private int facingDirection;
    
    // World bounds
    private int worldWidth;
    private int worldHeight;
    
    // Sprite sheet configuration
    private static final int FRAME_WIDTH = 90;
    private static final int FRAME_HEIGHT = 130;
    private static final int FRAMES_PER_ROW = 5;
    
    // Animation directions
    public static final int ANIM_DIR_UP = 0;
    public static final int ANIM_DIR_UP_RIGHT = 1;
    public static final int ANIM_DIR_RIGHT = 2;
    public static final int ANIM_DIR_DOWN_RIGHT = 3;
    public static final int ANIM_DIR_DOWN = 4;
    public static final int ANIM_DIR_DOWN_LEFT = 5;
    public static final int ANIM_DIR_LEFT = 6;
    public static final int ANIM_DIR_UP_LEFT = 7;
    
    // Animations
    private Animation idleAnim;
    private Animation runUpAnim;         // UP - Row 0
    private Animation runUpRightAnim;    // UP+RIGHT - Row 1
    private Animation runRightAnim;      // RIGHT - Row 2
    private Animation runDownRightAnim;  // DOWN+RIGHT - Row 3
    private Animation runDownAnim;       // DOWN - Row 4
    private Animation runDownLeftAnim;   // DOWN+LEFT - Row 5
    private Animation runLeftAnim;       // LEFT - Row 6
    private Animation runUpLeftAnim;     // UP+LEFT - Row 7
    private Animation currentAnimation;
    
    // Sound manager reference
    private SoundManager soundManager;
    
    /**
     * Utility method to clamp a value between min and max.
     */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
    
    public PlayerSprite(JPanel p, Player player, int xPos, int yPos, int worldW, int worldH) {
        super(p, xPos, yPos, 50, 50);
        playerData = player;
        
        worldX = xPos;
        worldY = yPos;
        preciseWorldX = xPos;
        preciseWorldY = yPos;
        screenX = xPos;
        screenY = yPos;
        preciseScreenX = xPos;
        preciseScreenY = yPos;
        damageFlashRemainingMs = 0;
        width = 50;
        height = 50;
        
        worldWidth = worldW;
        worldHeight = worldH;
        
        currentState = STATE_IDLE;
        facingDirection = DIR_RIGHT;
        
        // Initialize sound manager
        soundManager = SoundManager.getInstance();
        
        // Load player sprite strip and set up animations using auto-detection
        loadSpriteAnimations();
    }
    
    // Load sprite animations using StripAnimation class.
    private void loadSpriteAnimations() {
        // Use StripAnimation to load sprite strip and get animations
        Map<Integer, Animation> animations = StripAnimation.loadSpriteAnimations("images/player/playerRunningStrip.png", RUN_FRAME_DURATION);
        
        if (animations == null || animations.isEmpty()) {
            System.out.println("Failed to load playerRunningStrip.png, falling back to player.png");
            image = ImageManager.loadImage("player.png");
            return;
        }
        
        System.out.println("Sprite animations loaded successfully");
        
        // Set sprite dimensions
        width = StripAnimation.FRAME_WIDTH;
        height = StripAnimation.FRAME_HEIGHT;
        
        // Get animations from the map
        runUpAnim = animations.get(ANIM_DIR_UP);
        runUpRightAnim = animations.get(ANIM_DIR_UP_RIGHT);
        runRightAnim = animations.get(ANIM_DIR_RIGHT);
        runDownRightAnim = animations.get(ANIM_DIR_DOWN_RIGHT);
        runDownAnim = animations.get(ANIM_DIR_DOWN);
        runDownLeftAnim = animations.get(ANIM_DIR_DOWN_LEFT);
        runLeftAnim = animations.get(ANIM_DIR_LEFT);
        runUpLeftAnim = animations.get(ANIM_DIR_UP_LEFT);
        
        idleAnim = new Animation(true);
        if (runDownAnim != null) {
            // Use the first DOWN frame as the default idle pose.
            StripAnimation stripAnim = new StripAnimation();
            BufferedImage spriteStrip = ImageManager.loadBufferedImage("images/player/playerRunningStrip.png");
            if (spriteStrip != null) {
                BufferedImage[] downFrames = stripAnim.extractFramesFromRow(spriteStrip, ANIM_DIR_DOWN);
                if (downFrames.length > 0) {
                    idleAnim.addFrame(downFrames[0], 300);
                    image = downFrames[0];
                }
            }
        }
        
        // Set default animation
        currentAnimation = idleAnim;
        currentAnimation.start();
    }
    
    public void move(int direction, long deltaTimeMs) {
        if (!panel.isVisible()) return;
        
        Animation animationToPlay = null;
        double moveDistance = getCurrentMoveSpeed() * (deltaTimeMs / MOVEMENT_REFERENCE_FRAME_MS);
        
        switch (direction) {
            case DIR_LEFT:
                preciseWorldX -= moveDistance;
                facingDirection = DIR_LEFT;
                currentState = STATE_RUN;
                animationToPlay = runLeftAnim;
                break;
            case DIR_RIGHT:
                preciseWorldX += moveDistance;
                facingDirection = DIR_RIGHT;
                currentState = STATE_RUN;
                animationToPlay = runRightAnim;
                break;
            case DIR_UP:
                preciseWorldY -= moveDistance;
                facingDirection = DIR_UP;
                currentState = STATE_RUN;
                animationToPlay = runUpAnim;
                break;
            case DIR_DOWN:
                preciseWorldY += moveDistance;
                facingDirection = DIR_DOWN;
                currentState = STATE_RUN;
                animationToPlay = runDownAnim;
                break;
            case DIR_UP_LEFT:
                preciseWorldX -= moveDistance;
                preciseWorldY -= moveDistance;
                facingDirection = DIR_UP_LEFT;
                currentState = STATE_RUN;
                animationToPlay = runUpLeftAnim;
                break;
            case DIR_UP_RIGHT:
                preciseWorldX += moveDistance;
                preciseWorldY -= moveDistance;
                facingDirection = DIR_UP_RIGHT;
                currentState = STATE_RUN;
                animationToPlay = runUpRightAnim;
                break;
            case DIR_DOWN_LEFT:
                preciseWorldX -= moveDistance;
                preciseWorldY += moveDistance;
                facingDirection = DIR_DOWN_LEFT;
                currentState = STATE_RUN;
                animationToPlay = runDownLeftAnim;
                break;
            case DIR_DOWN_RIGHT:
                preciseWorldX += moveDistance;
                preciseWorldY += moveDistance;
                facingDirection = DIR_DOWN_RIGHT;
                currentState = STATE_RUN;
                animationToPlay = runDownRightAnim;
                break;
        }
        
        // Set animation and play sound
        setAnimationAndPlaySound(animationToPlay);
        
        // Clamp to world bounds
        preciseWorldX = clampToWorldBounds(preciseWorldX, width, worldWidth);
        preciseWorldY = clampToWorldBounds(preciseWorldY, height, worldHeight);
        syncPrecisePositionToWorldPosition();
    }
    
    private void setAnimationAndPlaySound(Animation anim) {
        if (anim != null) {
            currentAnimation = anim;
            if (!currentAnimation.isActive()) {
                currentAnimation.start();
            }
        }
        soundManager.startFootstep();
    }
    
    /**
     * Updates the screen position based on world position and camera offset.
     * Screen position = world position - camera position.
     */
    public void updateScreenPosition(int cameraX, int cameraY) {
        preciseScreenX = preciseWorldX - cameraX;
        preciseScreenY = preciseWorldY - cameraY;
        screenX = (int) Math.round(preciseScreenX);
        screenY = (int) Math.round(preciseScreenY);
    }
    
    public void setIdle() {
        currentState = STATE_IDLE;
        if (idleAnim != null) {
            currentAnimation = idleAnim;
            if (!currentAnimation.isActive()) {
                currentAnimation.start();
            }
        }
        // Stop footstep sound when player stops moving
        soundManager.stopFootstep();
    }
    
    public void update(long deltaTimeMs) {
        syncDimensionsWithCurrentFrame();
        if (currentAnimation != null) {
            currentAnimation.update(deltaTimeMs);
        }
    }

    public void update() {
        update(0);
    }
    
    // Draws the player at screen position.
    public void draw(Graphics2D g2) {
        syncDimensionsWithCurrentFrame();

        BufferedImage currentFrame = getCurrentBufferedImage();
        if (currentFrame != null) {
            BufferedImage frameToDraw = currentFrame;
            if (damageFlashRemainingMs > 0) {
                frameToDraw = ImageManager.tintVisiblePixels(currentFrame, Color.RED, 0.45f);
            }
            g2.drawImage(frameToDraw, screenX, screenY, width, height, null);
        }
    }
    
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
    
    public int getWorldX() {
        return worldX;
    }
    
    public int getWorldY() {
        return worldY;
    }
    
    public int getScreenX() {
        return screenX;
    }
    
    public int getScreenY() {
        return screenY;
    }

    public int getCenterX() {
        return worldX + width / 2;
    }

    public int getCenterY() {
        return worldY + height / 2;
    }
    

    
    /**
     * Activates the speed boost (3x speed for 1 seconds).
     * Multiple activations reset the timer.
     */
    public void activateSpeedBoost() {
        speedBoostActive = true;
        speedBoostTimer = SPEED_BOOST_DURATION;
    }
    
    public void updateSpeedBoost(long deltaTime) {
        if (speedBoostActive) {
            speedBoostTimer -= deltaTime;
            if (speedBoostTimer <= 0) {
                // Speed boost expired, reset to base speed
                speedBoostTimer = 0;
                speedBoostActive = false;
            }
        }
    }
    
    public void setWorldX(int x) {
        worldX = x;
        preciseWorldX = x;
    }
    
    public void setWorldY(int y) {
        worldY = y;
        preciseWorldY = y;
    }

    private void syncDimensionsWithCurrentFrame() {
        BufferedImage currentFrame = getCurrentBufferedImage();
        if (currentFrame != null) {
            width = currentFrame.getWidth();
            height = currentFrame.getHeight();
        }
    }

    private int getCurrentMoveSpeed() {
        int moveSpeed = playerData.getMoveSpeed();
        if (speedBoostActive) {
            return moveSpeed * SPEED_BOOST_MULTIPLIER;
        }
        return moveSpeed;
    }

    private double clampToWorldBounds(double value, int spriteSize, int worldSize) {
        return Math.max(0.0, Math.min(value, Math.max(0, worldSize - spriteSize)));
    }

    private void syncPrecisePositionToWorldPosition() {
        worldX = (int) Math.round(preciseWorldX);
        worldY = (int) Math.round(preciseWorldY);
    }

    public Player getPlayerData() {
        return playerData;
    }

    public void triggerDamageFlash() {
        damageFlashRemainingMs = DAMAGE_FLASH_DURATION_MS;
    }

    public void updateDamageFlash(long deltaTimeMs) {
        damageFlashRemainingMs = Math.max(0, damageFlashRemainingMs - deltaTimeMs);
    }
}

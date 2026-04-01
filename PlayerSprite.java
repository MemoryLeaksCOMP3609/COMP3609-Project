import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.swing.JPanel;

/**
 * Player sprite with keyboard movement (arrow keys/WASD) and animation states
 */
public class PlayerSprite extends Sprite {
    
    private int worldX;
    private int worldY;
    private int baseSpeed = 5; // Store the normal speed
    private int dx = baseSpeed;
    private int dy = baseSpeed;
    private boolean speedBoostActive = false;
    private long speedBoostTimer = 0; // Time remaining in milliseconds
    private static final long SPEED_BOOST_DURATION = 1000; // 1 second
    private static final int SPEED_BOOST_MULTIPLIER = 3; // 3x speed
    private int screenX;
    private int screenY;
    
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
    private static final int FRAME_WIDTH = 85;
    private static final int FRAME_HEIGHT = 120;
    private static final int FRAMES_PER_ROW = 16;
    
    // Animation directions
    public static final int ANIM_DIR_LEFT = 0;    // Row 0: running left (side view)
    public static final int ANIM_DIR_DOWN = 1;   // Row 1: running toward screen (front view)
    public static final int ANIM_DIR_DOWN_LEFT = 2; // Row 2: running 3/4 view
    
    // Animations
    private Animation idleAnim;
    private Animation runLeftAnim;       // LEFT - Row 0, no flip
    private Animation runRightAnim;      // RIGHT - Row 0, flipped
    private Animation runUpLeftAnim;     // UP+LEFT - Row 0, no flip
    private Animation runUpRightAnim;    // UP+RIGHT - Row 0, flipped
    private Animation runDownAnim;       // DOWN - Row 1, no flip
    private Animation runDownLeftAnim;   // DOWN+LEFT - Row 2, no flip
    private Animation runDownRightAnim;  // DOWN+RIGHT - Row 2, flipped
    private Animation currentAnimation;
    
    // Sound manager reference
    private SoundManager soundManager;
    
    /**
     * Utility method to clamp a value between min and max.
     */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
    
    public PlayerSprite(JPanel p, int xPos, int yPos, int worldW, int worldH) {
        super(p, xPos, yPos, 50, 50);
        
        worldX = xPos;
        worldY = yPos;
        screenX = xPos;
        screenY = yPos;
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
        Map<Integer, Animation> animations = StripAnimation.loadSpriteAnimations("images/playerRunningStrip.png", 80);
        
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
        runLeftAnim = animations.get(0);      // Left - Row 0, no flip
        runRightAnim = animations.get(1);     // Right - Row 0, flipped
        runUpLeftAnim = animations.get(2);    // Up+Left - Row 0, no flip
        runUpRightAnim = animations.get(3);   // Up+Right - Row 0, flipped
        runDownAnim = animations.get(4);      // Down - Row 1, no flip
        runDownLeftAnim = animations.get(5);  // Down+Left - Row 2, no flip
        runDownRightAnim = animations.get(6); // Down+Right - Row 2, flipped
        
        idleAnim = new Animation(true);
        if (runLeftAnim != null) {
            // Get the first frame from runLeftAnim for idle
            StripAnimation stripAnim = new StripAnimation();
            BufferedImage spriteStrip = ImageManager.loadBufferedImage("images/playerRunningStrip.png");
            if (spriteStrip != null) {
                BufferedImage[] row0Frames = stripAnim.extractFramesFromRow(spriteStrip, 0);
                if (row0Frames.length > 0) {
                    idleAnim.addFrame(row0Frames[0], 300);
                    image = row0Frames[0];
                }
            }
        }
        
        // Set default animation
        currentAnimation = idleAnim;
        currentAnimation.start();
    }
    
    public void move(int direction) {
        if (!panel.isVisible()) return;
        
        Animation animationToPlay = null;
        
        switch (direction) {
            case DIR_LEFT:
                worldX = worldX - dx;
                facingDirection = DIR_LEFT;
                currentState = STATE_RUN;
                animationToPlay = runLeftAnim;
                break;
            case DIR_RIGHT:
                worldX = worldX + dx;
                facingDirection = DIR_RIGHT;
                currentState = STATE_RUN;
                animationToPlay = runRightAnim;
                break;
            case DIR_UP:
                worldY = worldY - dy;
                currentState = STATE_RUN;
                animationToPlay = runRightAnim;
                break;
            case DIR_DOWN:
                worldY = worldY + dy;
                currentState = STATE_RUN;
                animationToPlay = runDownAnim;
                break;
            case DIR_UP_LEFT:
                worldX = worldX - dx;
                worldY = worldY - dy;
                facingDirection = DIR_UP_LEFT;
                currentState = STATE_RUN;
                animationToPlay = runUpLeftAnim;
                break;
            case DIR_UP_RIGHT:
                worldX = worldX + dx;
                worldY = worldY - dy;
                facingDirection = DIR_UP_RIGHT;
                currentState = STATE_RUN;
                animationToPlay = runUpRightAnim;
                break;
            case DIR_DOWN_LEFT:
                worldX = worldX - dx;
                worldY = worldY + dy;
                facingDirection = DIR_DOWN_LEFT;
                currentState = STATE_RUN;
                animationToPlay = runDownLeftAnim;
                break;
            case DIR_DOWN_RIGHT:
                worldX = worldX + dx;
                worldY = worldY + dy;
                facingDirection = DIR_DOWN_RIGHT;
                currentState = STATE_RUN;
                animationToPlay = runDownRightAnim;
                break;
        }
        
        // Set animation and play sound
        setAnimationAndPlaySound(animationToPlay);
        
        // Clamp to world bounds
        worldX = clamp(worldX, 0, worldWidth - width);
        worldY = clamp(worldY, 0, worldHeight - height);
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
        // Calculate screen position based on camera offset
        screenX = worldX - cameraX;
        screenY = worldY - cameraY;
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
    
    public void update() {
        if (currentAnimation != null) {
            currentAnimation.update();
        }
    }
    
    // Draws the player at screen position.
    public void draw(Graphics2D g2) {
        // Get the current frame image and update dimensions to match
        if (currentAnimation != null && currentAnimation.getImage() != null) {
            Image currentFrame = currentAnimation.getImage();
            width = currentFrame.getWidth(null);
            height = currentFrame.getHeight(null);
            g2.drawImage(currentFrame, screenX, screenY, width, height, null);
        } else if (image != null) {
            width = image.getWidth(null);
            height = image.getHeight(null);
            g2.drawImage(image, screenX, screenY, width, height, null);
        }
    }
    
    public Rectangle2D.Double getBoundingRectangle() {
        return new Rectangle2D.Double(worldX, worldY, width, height);
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
    

    
    /**
     * Activates the speed boost (3x speed for 1 seconds).
     * Multiple activations reset the timer.
     */
    public void activateSpeedBoost() {
        dx = baseSpeed * SPEED_BOOST_MULTIPLIER;
        dy = baseSpeed * SPEED_BOOST_MULTIPLIER;
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
                dx = baseSpeed;
                dy = baseSpeed;
            }
        }
    }
    
    public void setWorldX(int x) {
        worldX = x;
    }
    
    public void setWorldY(int y) {
        worldY = y;
    }
}

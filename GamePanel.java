import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends JPanel {
    
    // Game state
    private boolean gameRunning;
    private boolean gamePaused;
    private boolean gameOver;
    
    // World dimensions
    private int WORLD_WIDTH = 2500;
    private int WORLD_HEIGHT = 2500;
    
    // Player and sprites
    private PlayerSprite player;
    private ArrowSprite arrowSprite;
    private ArrayList<AnimatedSprite> animatedSprites;
    private ArrayList<SolidObject> solidObjects;
    private ArrayList<Collectible> collectibles;
    
    // Camera position
    private int cameraX;
    private int cameraY;
    
    // Key states
    private boolean leftKeyPressed;
    private boolean rightKeyPressed;
    private boolean upKeyPressed;
    private boolean downKeyPressed;
    
    private BufferedImage backgroundImage;
    
    private Random random;
    
    // World generator for entity creation
    private WorldGenerator worldGenerator;
    
    // Double buffering
    private BufferedImage doubleBufferImage;
    private Graphics2D doubleBufferG2;
    
    // Image effects
    private ArrayList<ImageFX> effects;
    private String activeEffectName;
    
    // Full screen grayscale effect for win condition
    private GrayScaleFX screenGrayScaleFX;
    
    // Collectibles tracking
    private int collectedCount;
    private int totalCollectibles;
    private static final int WIN_COLLECTIBLES = 5; // Number of collectibles required to win
    
    // FPS tracking
    private long lastFrameTime;
    private int fps;
    
    // Sound
    private SoundManager soundManager;
    
    // Golden tint effect for coin pickup
    private boolean goldenTintActive;
    private long goldenTintTimer;
    private static final long GOLDEN_TINT_DURATION = 1000;
    private static final int GOLDEN_TINT_COLOR = 0x80FFD700; // Semi-transparent golden (ARGB)
    
    // Game over exit timer
    private long gameOverTime;
    private static final long GAME_OVER_EXIT_DELAY = 1500;
    private boolean gameExiting;
    
    private InfoPanel infoPanel;
    
    // Game thread for precise timing
    private Thread gameThread;
    private volatile boolean gameThreadRunning;
    private static final int TARGET_FRAME_TIME = 40; // 40ms = 25 FPS
    
    public GamePanel() {
        this(null);
    }
    
    public GamePanel(InfoPanel info) {
        infoPanel = info;
        
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(800, 600));
        
        gameRunning = false;
        gamePaused = false;
        gameOver = false;
        
        leftKeyPressed = false;
        rightKeyPressed = false;
        upKeyPressed = false;
        downKeyPressed = false;
        
        cameraX = 0;
        cameraY = 0;
        
        collectedCount = 0;
        totalCollectibles = 0;
        
        animatedSprites = new ArrayList<AnimatedSprite>();
        solidObjects = new ArrayList<SolidObject>();
        collectibles = new ArrayList<Collectible>();
        effects = new ArrayList<ImageFX>();
        
        activeEffectName = "None";
        
        // Initialize grayscale effect
        screenGrayScaleFX = null;
        
        soundManager = SoundManager.getInstance();
        
        // Initialize random generator
        random = new Random();
        
        // Initialize world generator
        worldGenerator = new WorldGenerator(WORLD_WIDTH, WORLD_HEIGHT);
        worldGenerator.loadImages();
        
        // Initialize double buffering
        doubleBufferImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        doubleBufferG2 = doubleBufferImage.createGraphics();
        
        // Load background image
        backgroundImage = ImageManager.loadBufferedImage("images/worldBackgroundSmall.png");
        if (backgroundImage != null) {
            System.out.println("World background loaded: " + WORLD_WIDTH + "x" + WORLD_HEIGHT);
        } else {
            System.out.println("Failed to load worldBackgroundSmall.png, using default dimensions");
        }
        
        lastFrameTime = System.currentTimeMillis();
        fps = 0;
        
        // Initialize golden tint effect
        goldenTintActive = false;
        goldenTintTimer = 0;
        
        // Initialize game over exit timer
        gameOverTime = 0;
        gameExiting = false;
        
        // Initialize game thread
        gameThread = null;
        gameThreadRunning = false;
    }
    

    
    public void createGameEntities() {
        // Create player at world center
        int playerStartX = WORLD_WIDTH / 2 - 25;  // Center of world minus half player width
        int playerStartY = WORLD_HEIGHT / 2 - 25; // Center of world minus half player height
        player = new PlayerSprite(this, playerStartX, playerStartY, WORLD_WIDTH, WORLD_HEIGHT);
        
        // Use WorldGenerator to create solid objects
        solidObjects = worldGenerator.createSolidObjects(25, playerStartX, playerStartY, 250);
        
        // Use WorldGenerator to create collectibles
        collectibles = worldGenerator.createCollectibles(solidObjects, WIN_COLLECTIBLES, 40, 200);
        
        // Use WorldGenerator to create animated sprites
        animatedSprites = worldGenerator.createAnimatedSprites(this);
        
        // Create arrow sprite
        arrowSprite = new ArrowSprite();
        
        // Reset camera
        cameraX = 0;
        cameraY = 0;
        
        // Reset counters
        collectedCount = 0;
        totalCollectibles = collectibles.size();
    }
    
    public void startGame() {
        if (gameRunning) return;
        
        gameRunning = true;
        gamePaused = false;
        gameOver = false;
        activeEffectName = "None";
        
        // Reset grayscale effect and exit timer
        screenGrayScaleFX = null;
        gameOverTime = 0;
        gameExiting = false;
        
        createGameEntities();
        
        // Start background music at 60% volume
        soundManager.playBackgroundMusic();
        
        // Start the game thread
        startGameThread();
        
        repaint();
    }
    
    public void resetGame() {
        stopGame();
        gameOver = false;
        gameRunning = false;
        gamePaused = false;
        activeEffectName = "None";
        
        createGameEntities();
        
        startGame();
    }
    
    public void pauseGame() {
        gamePaused = !gamePaused;
        
        if (gamePaused) {
            soundManager.stopClip("background");
        } else {
            soundManager.playBackgroundMusic();
        }
    }
    
    public void stopGame() {
        gameRunning = false;
        soundManager.stopClip("background");
        stopGameThread();
    }
    
    // Starts the dedicated game thread for precise timing.
    private void startGameThread() {
        if (gameThread != null && gameThread.isAlive()) {
            return; // Thread already running
        }
        
        gameThreadRunning = true;
        gameThread = new Thread(new Runnable() {
            @Override
            public void run() {
                long lastFrameTime = System.nanoTime();
                final long targetNanos = TARGET_FRAME_TIME * 1_000_000; // Convert ms to ns
                
                while (gameThreadRunning && !Thread.currentThread().isInterrupted()) {
                    long currentTimeNanos = System.nanoTime();
                    long elapsedNanos = currentTimeNanos - lastFrameTime;
                    
                    // Calculate actual frame time for debug logging
                    long elapsedMs = elapsedNanos / 1_000_000;
                    
                    // Calculate delta time in milliseconds for game logic
                    long deltaTimeMs = elapsedMs;
                    
                    if (gameRunning && !gamePaused) {
                        // Update game logic
                        updatePlayer(deltaTimeMs);
                        checkCollisions();
                        updateEffects();
                        
                        // Update FPS counter from game thread (accurate timing)
                        updateFPSFromGameThread();
                        
                        // Request repaint on EDT
                        final long frameTime = elapsedNanos;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                repaint();
                            }
                        });
                    }
                    
                    // Check game over exit
                    if (gameOver && gameOverTime > 0) {
                        long elapsed = System.currentTimeMillis() - gameOverTime;
                        if (elapsed >= GAME_OVER_EXIT_DELAY) {
                            System.exit(0);
                        }
                    }
                    
                    // Update local and class-level lastFrameTime for the game loop
                    lastFrameTime = currentTimeNanos;
                    GamePanel.this.lastFrameTime = System.currentTimeMillis();
                    
                    // Sleep for the remaining time to maintain target frame rate
                    long sleepTimeNanos = targetNanos - (System.nanoTime() - currentTimeNanos);
                    if (sleepTimeNanos > 0) {
                        try {
                            Thread.sleep(sleepTimeNanos / 1_000_000, (int)(sleepTimeNanos % 1_000_000));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        });
        gameThread.setName("GameLoopThread");
        gameThread.setPriority(Thread.MAX_PRIORITY);
        gameThread.start();
    }

    private void stopGameThread() {
        gameThreadRunning = false;
        if (gameThread != null) {
            gameThread.interrupt();
            try {
                gameThread.join(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            gameThread = null;
        }
    }
    
    public void triggerGameOver(boolean won) {
        gameOver = true;
        gameRunning = false;
        soundManager.stopAll();
        
        // keep game time running to check elapsed time
        // Set game over timestamp for exit timer
        gameOverTime = System.currentTimeMillis();
        gameExiting = false;
        
        if (won) {
            activeEffectName = "GrayScale";
            // Create full-screen grayscale effect for the background
            if (backgroundImage != null) {
                BufferedImage grayBg = new BufferedImage(backgroundImage.getWidth(), backgroundImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = grayBg.createGraphics();
                g2.drawImage(backgroundImage, 0, 0, null);
                g2.dispose();
                
                // Convert to grayscale
                int[] pixels = new int[grayBg.getWidth() * grayBg.getHeight()];
                grayBg.getRGB(0, 0, grayBg.getWidth(), grayBg.getHeight(), pixels, 0, grayBg.getWidth());
                for (int i = 0; i < pixels.length; i++) {
                    int alpha = (pixels[i] >> 24) & 255;
                    int red = (pixels[i] >> 16) & 255;
                    int green = (pixels[i] >> 8) & 255;
                    int blue = pixels[i] & 255;
                    int gray = (int)(0.299 * red + 0.587 * green + 0.114 * blue);
                    pixels[i] = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
                }
                grayBg.setRGB(0, 0, grayBg.getWidth(), grayBg.getHeight(), pixels, 0, grayBg.getWidth());
                
                screenGrayScaleFX = new GrayScaleFX(0, 0, WORLD_WIDTH, WORLD_HEIGHT, backgroundImage, grayBg);
            }
        }
        
        repaint();
    }
    
    public void updatePlayer(long deltaTime) {
        if (player == null || !gameRunning || gamePaused) return;
        
        // Update speed boost timer
        player.updateSpeedBoost(deltaTime);
        
        // Update golden tint timer
        if (goldenTintActive) {
            goldenTintTimer -= deltaTime;
            if (goldenTintTimer <= 0) {
                goldenTintActive = false;
                goldenTintTimer = 0;
                activeEffectName = "None";
            }
        }
        
        // Store old world position for collision reversion
        int oldWorldX = player.getWorldX();
        int oldWorldY = player.getWorldY();
        
        int moveDirection = 0;
        
        // Check diagonal directions first
        if (upKeyPressed && leftKeyPressed) {
            moveDirection = PlayerSprite.DIR_UP_LEFT;
        } else if (upKeyPressed && rightKeyPressed) {
            moveDirection = PlayerSprite.DIR_UP_RIGHT;
        } else if (downKeyPressed && leftKeyPressed) {
            moveDirection = PlayerSprite.DIR_DOWN_LEFT;
        } else if (downKeyPressed && rightKeyPressed) {
            moveDirection = PlayerSprite.DIR_DOWN_RIGHT;
        } else if (leftKeyPressed && !rightKeyPressed) {
            moveDirection = PlayerSprite.DIR_LEFT;
        } else if (rightKeyPressed && !leftKeyPressed) {
            moveDirection = PlayerSprite.DIR_RIGHT;
        } else if (upKeyPressed && !downKeyPressed) {
            moveDirection = PlayerSprite.DIR_UP;
        } else if (downKeyPressed && !upKeyPressed) {
            moveDirection = PlayerSprite.DIR_DOWN;
        }
        
        // Move player in the determined direction
        if (moveDirection != 0) {
            player.move(moveDirection);
            
            // Check collision after movement
            Rectangle2D.Double playerBounds = player.getBoundingRectangle();
            for (SolidObject solid : solidObjects) {
                if (playerBounds.intersects(solid.getBoundingRectangle())) {
                    // Revert position
                    player.setWorldX(oldWorldX);
                    player.setWorldY(oldWorldY);
                    break;
                }
            }
        }
        
        if (!leftKeyPressed && !rightKeyPressed && !upKeyPressed && !downKeyPressed) {
            player.setIdle();
        }
        
        // Update player animation
        player.update();
        
        // Update camera to follow player
        updateCamera();
        
        // Update player screen position
        player.updateScreenPosition(cameraX, cameraY);
        
        // Update animated sprites
        for (AnimatedSprite sprite : animatedSprites) {
            sprite.updateScreenPosition(cameraX, cameraY);
            sprite.update();
        }
        
        // Update collectibles screen positions and animations
        for (Collectible collectible : collectibles) {
            collectible.updateScreenPosition(cameraX, cameraY);
            collectible.update();
        }
        
        // Update arrow sprite
        if (arrowSprite != null) {
            arrowSprite.update(player.getScreenX(), player.getScreenY(), collectibles);
        }
        
        // Play movement sound
        if ((leftKeyPressed || rightKeyPressed || upKeyPressed || downKeyPressed) && 
            soundManager != null && !soundManager.isPlaying("footstep")) {
        }
        
        repaint();
    }
    
    /**
     * Updates the camera position to follow the player.
     * 
     * Camera System:
     * - When player can move freely, player stays centered on screen
     * - When camera would go past world edges (0,0 or WORLD_WIDTH/WORLD_HEIGHT), 
     *   camera is clamped and player appears at edge instead of centered
     * - Camera is clamped to world boundaries
     */
    private void updateCamera() {
        // Calculate ideal camera position to center player on screen
        int playerWidth = player.getWidth();
        int playerHeight = player.getHeight();
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        
        // Center camera on player
        cameraX = player.getWorldX() - panelWidth / 2 + playerWidth / 2;
        cameraY = player.getWorldY() - panelHeight / 2 + playerHeight / 2;
        
        // Clamp camera to world boundaries
        cameraX = Math.max(0, Math.min(cameraX, WORLD_WIDTH - panelWidth));
        cameraY = Math.max(0, Math.min(cameraY, WORLD_HEIGHT - panelHeight));
    }
    
    public void checkCollisions() {
        if (player == null || !gameRunning || gamePaused) return;
        
        Rectangle2D.Double playerBounds = player.getBoundingRectangle();
        
        // Check collectible collisions
        for (Collectible collectible : collectibles) {
            if (!collectible.isCollected() && 
                playerBounds.intersects(collectible.getBoundingRectangle())) {
                collectible.collect();
                collectedCount++;
                soundManager.playClip("coinPickup", false);
                
                // Activate speed boost (3x speed for 3 seconds)
                player.activateSpeedBoost();
                
                // Activate golden tint effect for 1 second
                goldenTintActive = true;
                goldenTintTimer = GOLDEN_TINT_DURATION;
                activeEffectName = "Golden Tint";
                
                // Check win condition
                if (collectedCount >= WIN_COLLECTIBLES) {
                    triggerGameOver(true);
                }
            }
        }
    }
    
    public void updateEffects() {
        for (ImageFX effect : effects) {
            effect.update();
        }
        
        // Update screen grayscale effect
        if (screenGrayScaleFX != null) {
            screenGrayScaleFX.update();
        }
        
        // Update info panel
        if (infoPanel != null) {
            if (player != null) {
                infoPanel.updatePlayerPosition(player.getWorldX(), player.getWorldY());
            }
            infoPanel.updateFPS(fps);
            infoPanel.updateCollectibles(collectedCount, totalCollectibles);
            infoPanel.updateActiveEffects(activeEffectName);
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        
        // Check if we should exit the game (3 seconds after game over)
        if (gameOver && gameOverTime > 0 && !gameExiting) {
            long elapsed = System.currentTimeMillis() - gameOverTime;
            if (elapsed >= GAME_OVER_EXIT_DELAY) {
                gameExiting = true;
                // Exit the game
                System.exit(0);
            }
        }
        
        // double buffering
        if (doubleBufferImage != null) {
            drawToBuffer(doubleBufferG2);
            g.drawImage(doubleBufferImage, 0, 0, null);
        } else {
            drawToBuffer(g2);
        }
    }
    
    private void drawToBuffer(Graphics2D g2) {
        // Check if we should apply grayscale effect (when all coins are collected and game over)
        boolean applyGrayScale = (gameOver && collectedCount >= WIN_COLLECTIBLES && screenGrayScaleFX != null);
        
        // Clear background
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, -cameraX, -cameraY, WORLD_WIDTH, WORLD_HEIGHT, null);
        }
        
        if (!gameRunning && !gameOver) {
            // Draw start screen
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 36));
            g2.drawString("Coin Collector", 275, 280);
            g2.setFont(new Font("Arial", Font.PLAIN, 18));
            g2.drawString("Press Start to begin", 320, 330);
            return;
        }
        
        // Draw solid objects (25 trees/rocks)
        g2.setColor(new Color(100, 100, 100));
        for (SolidObject solid : solidObjects) {
            solid.draw(g2, cameraX, cameraY);
        }
        
        // Draw collectibles
        for (Collectible collectible : collectibles) {
            collectible.draw(g2);
        }
        
        // Draw animated sprites
        for (AnimatedSprite sprite : animatedSprites) {
            sprite.draw(g2);
        }
        
        // Draw player
        if (player != null) {
            player.draw(g2);
        }
        
        // Draw arrow sprite
        if (arrowSprite != null) {
            arrowSprite.draw(g2);
        }
        
        // Draw image effects
        for (ImageFX effect : effects) {
            effect.draw(g2);
        }
        
        // Draw golden tint overlay if active, overridden by grayscale
        if (goldenTintActive && !applyGrayScale) {
            g2.setColor(new Color(
                (GOLDEN_TINT_COLOR >> 16) & 0xFF,
                (GOLDEN_TINT_COLOR >> 8) & 0xFF,
                GOLDEN_TINT_COLOR & 0xFF,
                (GOLDEN_TINT_COLOR >> 24) & 0xFF
            ));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // Apply grayscale to entire screen
        if (applyGrayScale && doubleBufferImage != null) {
            int width = getWidth();
            int height = getHeight();
            
            // Create a copy to convert to grayscale
            BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gGray = grayImage.createGraphics();
            gGray.drawImage(doubleBufferImage, 0, 0, null);
            gGray.dispose();
            
            // Convert to grayscale using pixel manipulation
            int[] pixels = new int[width * height];
            grayImage.getRGB(0, 0, width, height, pixels, 0, width);
            
            for (int i = 0; i < pixels.length; i++) {
                int alpha = (pixels[i] >> 24) & 255;
                int red = (pixels[i] >> 16) & 255;
                int green = (pixels[i] >> 8) & 255;
                int blue = pixels[i] & 255;
                
                // Standard grayscale conversion formula
                int gray = (int)(0.299 * red + 0.587 * green + 0.114 * blue);
                
                pixels[i] = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
            }
            
            grayImage.setRGB(0, 0, width, height, pixels, 0, width);
            
            // Draw the grayscale version
            g2.drawImage(grayImage, 0, 0, null);
        }
        
        // Draw game over screen
        if (gameOver) {
            if (!applyGrayScale) {
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            g2.drawString("Game Over", 270, 280);
            
        }
    }
    
    /**
     * Updates FPS directly from the game thread with accurate timing.
     * Called by the game thread each frame.
     */
    private void updateFPSFromGameThread() {
        // Use the actual frame time from the game loop
        fps = 1000 / TARGET_FRAME_TIME;
    }
    
    // Key state setters
    public void setLeftKeyPressed(boolean pressed) {
        leftKeyPressed = pressed;
    }
    
    public void setRightKeyPressed(boolean pressed) {
        rightKeyPressed = pressed;
    }
    
    public void setUpKeyPressed(boolean pressed) {
        upKeyPressed = pressed;
    }
    
    public void setDownKeyPressed(boolean pressed) {
        downKeyPressed = pressed;
    }
    
    // Game state getters
    public boolean isGameRunning() {
        return gameRunning;
    }
    
    public boolean isGamePaused() {
        return gamePaused;
    }
    
    public boolean isGameOver() {
        return gameOver;
    }
    
    public PlayerSprite getPlayer() {
        return player;
    }
    
    public int getFPS() {
        return fps;
    }
    
    public int getCollectedCount() {
        return collectedCount;
    }
    
    public int getTotalCollectibles() {
        return totalCollectibles;
    }
    
    public String getActiveEffectName() {
        return activeEffectName;
    }
}

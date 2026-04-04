import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class GamePanel extends JPanel {
    private static final int EMBEDDED_PIXEL_THRESHOLD = 12;
    private static final int MAX_PUSH_OUT_DISTANCE = 48;
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private static final int WORLD_WIDTH = 2500;
    private static final int WORLD_HEIGHT = 2500;
    private static final int WIN_COLLECTIBLES = 5;
    private static final long GOLDEN_TINT_DURATION = 1000;
    private static final int GOLDEN_TINT_COLOR = 0x80FFD700;
    private static final long GAME_OVER_EXIT_DELAY = 1500;
    private static final int TARGET_FRAME_TIME = 40;

    private final GameSessionState sessionState;
    private final GameInputState inputState;
    private final GameWorld world;

    private BufferedImage doubleBufferImage;
    private Graphics2D doubleBufferG2;

    private ArrayList<ImageFX> effects;
    private GrayScaleFX screenGrayScaleFX;
    private long lastFrameTime;
    private SoundManager soundManager;
    private InfoPanel infoPanel;
    private Thread gameThread;
    private volatile boolean gameThreadRunning;

    public GamePanel() {
        this(null);
    }

    public GamePanel(InfoPanel info) {
        infoPanel = info;
        sessionState = new GameSessionState();
        inputState = new GameInputState();
        world = new GameWorld(WORLD_WIDTH, WORLD_HEIGHT);

        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

        effects = new ArrayList<ImageFX>();
        screenGrayScaleFX = null;
        soundManager = SoundManager.getInstance();

        doubleBufferImage = new BufferedImage(PANEL_WIDTH, PANEL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        doubleBufferG2 = doubleBufferImage.createGraphics();

        if (world.getBackgroundImage() != null) {
            System.out.println("World background loaded: " + WORLD_WIDTH + "x" + WORLD_HEIGHT);
        } else {
            System.out.println("Failed to load worldBackgroundSmall.png, using default dimensions");
        }

        lastFrameTime = System.currentTimeMillis();
        gameThread = null;
        gameThreadRunning = false;
    }

    public void createGameEntities() {
        world.initializeEntities(this, WIN_COLLECTIBLES);
        sessionState.setCollectedCount(0);
        sessionState.setTotalCollectibles(world.getCollectibles().size());
    }

    public void startGame() {
        if (sessionState.isGameRunning()) {
            return;
        }

        sessionState.resetForNewGame();
        screenGrayScaleFX = null;
        createGameEntities();
        soundManager.playBackgroundMusic();
        startGameThread();
        repaint();
    }

    public void resetGame() {
        stopGame();
        sessionState.resetForNewPanel();
        startGame();
    }

    public void pauseGame() {
        sessionState.setGamePaused(!sessionState.isGamePaused());

        if (sessionState.isGamePaused()) {
            soundManager.stopClip("background");
        } else {
            soundManager.playBackgroundMusic();
        }
    }

    public void stopGame() {
        sessionState.setGameRunning(false);
        soundManager.stopClip("background");
        stopGameThread();
    }

    private void startGameThread() {
        if (gameThread != null && gameThread.isAlive()) {
            return;
        }

        gameThreadRunning = true;
        gameThread = new Thread(new Runnable() {
            @Override
            public void run() {
                long frameStartedAt = System.nanoTime();
                final long targetNanos = TARGET_FRAME_TIME * 1_000_000;

                while (gameThreadRunning && !Thread.currentThread().isInterrupted()) {
                    long currentTimeNanos = System.nanoTime();
                    long elapsedNanos = currentTimeNanos - frameStartedAt;
                    long deltaTimeMs = elapsedNanos / 1_000_000;

                    if (sessionState.isGameRunning() && !sessionState.isGamePaused()) {
                        updatePlayer(deltaTimeMs);
                        checkCollisions();
                        updateEffects();
                        updateFPSFromGameThread();

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                repaint();
                            }
                        });
                    }

                    if (sessionState.isGameOver() && sessionState.getGameOverTime() > 0) {
                        long elapsed = System.currentTimeMillis() - sessionState.getGameOverTime();
                        if (elapsed >= GAME_OVER_EXIT_DELAY) {
                            System.exit(0);
                        }
                    }

                    frameStartedAt = currentTimeNanos;
                    lastFrameTime = System.currentTimeMillis();

                    long sleepTimeNanos = targetNanos - (System.nanoTime() - currentTimeNanos);
                    if (sleepTimeNanos > 0) {
                        try {
                            Thread.sleep(sleepTimeNanos / 1_000_000, (int) (sleepTimeNanos % 1_000_000));
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
        sessionState.setGameOver(true);
        sessionState.setGameRunning(false);
        soundManager.stopAll();
        sessionState.setGameOverTime(System.currentTimeMillis());
        sessionState.setGameExiting(false);

        if (won) {
            sessionState.setActiveEffectName("GrayScale");
            if (world.getBackgroundImage() != null) {
                BufferedImage backgroundImage = world.getBackgroundImage();
                BufferedImage grayBg = new BufferedImage(backgroundImage.getWidth(), backgroundImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = grayBg.createGraphics();
                g2.drawImage(backgroundImage, 0, 0, null);
                g2.dispose();

                int[] pixels = new int[grayBg.getWidth() * grayBg.getHeight()];
                grayBg.getRGB(0, 0, grayBg.getWidth(), grayBg.getHeight(), pixels, 0, grayBg.getWidth());
                for (int i = 0; i < pixels.length; i++) {
                    int alpha = (pixels[i] >> 24) & 255;
                    int red = (pixels[i] >> 16) & 255;
                    int green = (pixels[i] >> 8) & 255;
                    int blue = pixels[i] & 255;
                    int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                    pixels[i] = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
                }
                grayBg.setRGB(0, 0, grayBg.getWidth(), grayBg.getHeight(), pixels, 0, grayBg.getWidth());
                screenGrayScaleFX = new GrayScaleFX(0, 0, WORLD_WIDTH, WORLD_HEIGHT, backgroundImage, grayBg);
            }
        }

        repaint();
    }

    public void updatePlayer(long deltaTime) {
        PlayerSprite player = world.getPlayer();
        if (player == null || !sessionState.isGameRunning() || sessionState.isGamePaused()) {
            return;
        }

        player.updateSpeedBoost(deltaTime);

        if (sessionState.isGoldenTintActive()) {
            long remainingGoldenTint = sessionState.getGoldenTintTimer() - deltaTime;
            sessionState.setGoldenTintTimer(remainingGoldenTint);
            if (remainingGoldenTint <= 0) {
                sessionState.setGoldenTintActive(false);
                sessionState.setGoldenTintTimer(0);
                sessionState.setActiveEffectName("None");
            }
        }

        int oldWorldX = player.getWorldX();
        int oldWorldY = player.getWorldY();
        int moveDirection = inputState.resolveMovementDirection();

        if (moveDirection != 0) {
            player.move(moveDirection);

            if (!resolvePlayerSolidCollision(oldWorldX, oldWorldY, moveDirection)) {
                player.setWorldX(oldWorldX);
                player.setWorldY(oldWorldY);
            }
        }

        if (!inputState.isAnyMovementPressed()) {
            player.setIdle();
        }

        player.update();
        world.updateCamera(getWidth(), getHeight());
        world.updateScreenPositions();
        repaint();
    }

    public void checkCollisions() {
        PlayerSprite player = world.getPlayer();
        if (player == null || !sessionState.isGameRunning() || sessionState.isGamePaused()) {
            return;
        }

        Rectangle2D.Double playerBounds = player.getBoundingRectangle();

        for (Collectible collectible : world.getCollectibles()) {
            if (!collectible.isCollected()
                && PixelCollision.intersects(playerBounds, player.getCurrentBufferedImage(),
                collectible.getBoundingRectangle(), collectible.getCurrentBufferedImage())) {
                collectible.collect();
                sessionState.incrementCollectedCount();
                soundManager.playClip("coinPickup", false);
                player.activateSpeedBoost();
                sessionState.setGoldenTintActive(true);
                sessionState.setGoldenTintTimer(GOLDEN_TINT_DURATION);
                sessionState.setActiveEffectName("Golden Tint");

                if (sessionState.getCollectedCount() >= WIN_COLLECTIBLES) {
                    triggerGameOver(true);
                }
            }
        }
    }

    private boolean resolvePlayerSolidCollision(int fallbackX, int fallbackY, int moveDirection) {
        PlayerSprite player = world.getPlayer();
        Rectangle2D.Double playerBounds = player.getBoundingRectangle();
        BufferedImage playerImage = player.getCurrentBufferedImage();

        for (SolidObject solid : world.getSolidObjects()) {
            int overlapPixels = PixelCollision.countOverlappingPixels(playerBounds, playerImage,
                solid.getBoundingRectangle(), solid.getImage(), EMBEDDED_PIXEL_THRESHOLD);
            if (overlapPixels > 0) {
                if (overlapPixels >= EMBEDDED_PIXEL_THRESHOLD
                    && pushPlayerOutOfSolid(solid, moveDirection, fallbackX, fallbackY)) {
                    return true;
                }
                return false;
            }
        }

        return true;
    }

    private boolean pushPlayerOutOfSolid(SolidObject solid, int moveDirection, int fallbackX, int fallbackY) {
        PlayerSprite player = world.getPlayer();
        int originalX = player.getWorldX();
        int originalY = player.getWorldY();
        int[] reverseDirection = getReverseStep(moveDirection);

        if (reverseDirection[0] == 0 && reverseDirection[1] == 0) {
            return placePlayerAt(fallbackX, fallbackY);
        }

        for (int step = 1; step <= MAX_PUSH_OUT_DISTANCE; step++) {
            int candidateX = originalX + reverseDirection[0] * step;
            int candidateY = originalY + reverseDirection[1] * step;

            if (placePlayerAt(candidateX, candidateY) && !isPlayerCollidingWithSolid(solid)) {
                return true;
            }
        }

        return placePlayerAt(fallbackX, fallbackY);
    }

    private boolean isPlayerCollidingWithSolid(SolidObject solid) {
        PlayerSprite player = world.getPlayer();
        return PixelCollision.intersects(player.getBoundingRectangle(), player.getCurrentBufferedImage(),
            solid.getBoundingRectangle(), solid.getImage());
    }

    private boolean placePlayerAt(int worldX, int worldY) {
        PlayerSprite player = world.getPlayer();
        player.setWorldX(worldX);
        player.setWorldY(worldY);
        return true;
    }

    private int[] getReverseStep(int moveDirection) {
        switch (moveDirection) {
            case PlayerSprite.DIR_LEFT:
                return new int[] { 1, 0 };
            case PlayerSprite.DIR_RIGHT:
                return new int[] { -1, 0 };
            case PlayerSprite.DIR_UP:
                return new int[] { 0, 1 };
            case PlayerSprite.DIR_DOWN:
                return new int[] { 0, -1 };
            case PlayerSprite.DIR_UP_LEFT:
                return new int[] { 1, 1 };
            case PlayerSprite.DIR_UP_RIGHT:
                return new int[] { -1, 1 };
            case PlayerSprite.DIR_DOWN_LEFT:
                return new int[] { 1, -1 };
            case PlayerSprite.DIR_DOWN_RIGHT:
                return new int[] { -1, -1 };
            default:
                return new int[] { 0, 0 };
        }
    }

    public void updateEffects() {
        for (ImageFX effect : effects) {
            effect.update();
        }

        if (screenGrayScaleFX != null) {
            screenGrayScaleFX.update();
        }

        if (infoPanel != null) {
            PlayerSprite player = world.getPlayer();
            if (player != null) {
                infoPanel.updatePlayerPosition(player.getWorldX(), player.getWorldY());
            }
            infoPanel.updateFPS(sessionState.getFps());
            infoPanel.updateCollectibles(sessionState.getCollectedCount(), sessionState.getTotalCollectibles());
            infoPanel.updateActiveEffects(sessionState.getActiveEffectName());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (sessionState.isGameOver() && sessionState.getGameOverTime() > 0 && !sessionState.isGameExiting()) {
            long elapsed = System.currentTimeMillis() - sessionState.getGameOverTime();
            if (elapsed >= GAME_OVER_EXIT_DELAY) {
                sessionState.setGameExiting(true);
                System.exit(0);
            }
        }

        if (doubleBufferImage != null) {
            drawToBuffer(doubleBufferG2);
            g.drawImage(doubleBufferImage, 0, 0, null);
        } else {
            drawToBuffer((Graphics2D) g);
        }
    }

    private void drawToBuffer(Graphics2D g2) {
        boolean applyGrayScale = sessionState.isGameOver()
            && sessionState.getCollectedCount() >= WIN_COLLECTIBLES
            && screenGrayScaleFX != null;

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (world.getBackgroundImage() != null) {
            g2.drawImage(world.getBackgroundImage(), -world.getCameraX(), -world.getCameraY(), WORLD_WIDTH, WORLD_HEIGHT, null);
        }

        if (!sessionState.isGameRunning() && !sessionState.isGameOver()) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 36));
            g2.drawString("Coin Collector", 275, 280);
            g2.setFont(new Font("Arial", Font.PLAIN, 18));
            g2.drawString("Press Start to begin", 320, 330);
            return;
        }

        g2.setColor(new Color(100, 100, 100));
        for (SolidObject solid : world.getSolidObjects()) {
            solid.draw(g2, world.getCameraX(), world.getCameraY());
        }

        for (Collectible collectible : world.getCollectibles()) {
            collectible.draw(g2);
        }

        for (AnimatedSprite sprite : world.getAnimatedSprites()) {
            sprite.draw(g2);
        }

        if (world.getPlayer() != null) {
            world.getPlayer().draw(g2);
        }

        if (world.getArrowSprite() != null) {
            world.getArrowSprite().draw(g2);
        }

        for (ImageFX effect : effects) {
            effect.draw(g2);
        }

        if (sessionState.isGoldenTintActive() && !applyGrayScale) {
            g2.setColor(new Color(
                (GOLDEN_TINT_COLOR >> 16) & 0xFF,
                (GOLDEN_TINT_COLOR >> 8) & 0xFF,
                GOLDEN_TINT_COLOR & 0xFF,
                (GOLDEN_TINT_COLOR >> 24) & 0xFF
            ));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        if (applyGrayScale && doubleBufferImage != null) {
            int width = getWidth();
            int height = getHeight();
            BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gGray = grayImage.createGraphics();
            gGray.drawImage(doubleBufferImage, 0, 0, null);
            gGray.dispose();

            int[] pixels = new int[width * height];
            grayImage.getRGB(0, 0, width, height, pixels, 0, width);

            for (int i = 0; i < pixels.length; i++) {
                int alpha = (pixels[i] >> 24) & 255;
                int red = (pixels[i] >> 16) & 255;
                int green = (pixels[i] >> 8) & 255;
                int blue = pixels[i] & 255;
                int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                pixels[i] = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
            }

            grayImage.setRGB(0, 0, width, height, pixels, 0, width);
            g2.drawImage(grayImage, 0, 0, null);
        }

        if (sessionState.isGameOver()) {
            if (!applyGrayScale) {
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            g2.drawString("Game Over", 270, 280);
        }
    }

    private void updateFPSFromGameThread() {
        sessionState.setFps(1000 / TARGET_FRAME_TIME);
    }

    public void setLeftKeyPressed(boolean pressed) {
        inputState.setLeftPressed(pressed);
    }

    public void setRightKeyPressed(boolean pressed) {
        inputState.setRightPressed(pressed);
    }

    public void setUpKeyPressed(boolean pressed) {
        inputState.setUpPressed(pressed);
    }

    public void setDownKeyPressed(boolean pressed) {
        inputState.setDownPressed(pressed);
    }

    public boolean isGameRunning() {
        return sessionState.isGameRunning();
    }

    public boolean isGamePaused() {
        return sessionState.isGamePaused();
    }

    public boolean isGameOver() {
        return sessionState.isGameOver();
    }

    public PlayerSprite getPlayer() {
        return world.getPlayer();
    }

    public int getFPS() {
        return sessionState.getFps();
    }

    public int getCollectedCount() {
        return sessionState.getCollectedCount();
    }

    public int getTotalCollectibles() {
        return sessionState.getTotalCollectibles();
    }

    public String getActiveEffectName() {
        return sessionState.getActiveEffectName();
    }
}

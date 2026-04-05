import javax.swing.JPanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel {
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private static final int WORLD_WIDTH = 2500;
    private static final int WORLD_HEIGHT = 2500;
    private static final int TOTAL_COLLECTIBLES = 11;
    private static final int EXPERIENCE_PER_COLLECTIBLE = 25;
    private static final long GOLDEN_TINT_DURATION = 1000;
    private static final int GOLDEN_TINT_COLOR = 0x80FFD700;
    private static final long GAME_OVER_EXIT_DELAY = 1500;
    private static final int TARGET_FPS = 60;
    private static final long TARGET_FRAME_NANOS = 1_000_000_000L / TARGET_FPS;
    private static final int TIMER_POLL_DELAY_MS = 1;
    private static final int MAX_UPDATES_PER_TICK = 4;
    private static final int MAX_PUSH_OUT_DISTANCE = 48;
    private static final int BAT_STOP_DISTANCE = 200;
    private static final double BAT_BULLET_SPEED = 8.0;
    private static final long BAT_FIRE_INTERVAL_MS = 1000;

    private final GameSessionState sessionState;
    private final GameInputState inputState;
    private final GameWorld world;
    private final PlayerCollisionResolver playerCollisionResolver;
    private final GameCombatSystem combatSystem;
    private final GamePanelRenderer renderer;
    private final GameLoopMetrics loopMetrics;
    private final LevelUpManager levelUpManager;
    private final Object stateLock;

    private BufferedImage doubleBufferImage;
    private Graphics2D doubleBufferG2;

    private ArrayList<ImageFX> effects;
    private GrayScaleFX screenGrayScaleFX;
    private SoundManager soundManager;
    private InfoPanel infoPanel;
    private Thread gameLoopThread;
    private volatile boolean gameLoopRunning;
    private WeaponType selectedWeapon;
    private int viewportWidth;
    private int viewportHeight;

    public GamePanel() {
        this(null);
    }

    public GamePanel(InfoPanel info) {
        infoPanel = info;
        sessionState = new GameSessionState();
        inputState = new GameInputState();
        world = new GameWorld(WORLD_WIDTH, WORLD_HEIGHT);
        playerCollisionResolver = new PlayerCollisionResolver(MAX_PUSH_OUT_DISTANCE);
        combatSystem = new GameCombatSystem(
            world,
            sessionState,
            SoundManager.getInstance(),
            GOLDEN_TINT_DURATION,
            BAT_FIRE_INTERVAL_MS,
            EXPERIENCE_PER_COLLECTIBLE,
            BAT_STOP_DISTANCE,
            BAT_BULLET_SPEED
        );
        renderer = new GamePanelRenderer(WORLD_WIDTH, WORLD_HEIGHT, GOLDEN_TINT_COLOR);
        loopMetrics = new GameLoopMetrics();
        levelUpManager = new LevelUpManager();
        stateLock = new Object();

        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setFocusable(true);

        effects = new ArrayList<ImageFX>();
        screenGrayScaleFX = null;
        soundManager = SoundManager.getInstance();
        viewportWidth = PANEL_WIDTH;
        viewportHeight = PANEL_HEIGHT;

        ensureRenderBuffer(PANEL_WIDTH, PANEL_HEIGHT);

        if (world.getBackgroundImage() != null) {
            System.out.println("World background loaded: " + WORLD_WIDTH + "x" + WORLD_HEIGHT);
        } else {
            System.out.println("Failed to load worldBackgroundSmall.png, using default dimensions");
        }

        gameLoopThread = null;
        gameLoopRunning = false;
        selectedWeapon = WeaponType.FIRE_ARROW;
    }

    public void createGameEntities() {
        synchronized (stateLock) {
            world.initializeEntities(this, TOTAL_COLLECTIBLES);
            sessionState.setCollectedCount(0);
            sessionState.setTotalCollectibles(world.getCollectibles().size());
        }
    }

    public void startGame() {
        synchronized (stateLock) {
            if (sessionState.isGameRunning()) {
                return;
            }

            sessionState.resetForNewGame();
            screenGrayScaleFX = null;
            createGameEntities();
            loopMetrics.reset();
            levelUpManager.reset();
            sessionState.setFps(0);
            if (world.getPlayerData() != null) {
                world.getPlayerData().setWeaponType(selectedWeapon);
            }
            soundManager.playBackgroundMusic();
        }
        startGameThread();
    }

    public void resetGame() {
        stopGame();
        sessionState.resetForNewPanel();
        startGame();
    }

    public void pauseGame() {
        synchronized (stateLock) {
            sessionState.setGamePaused(!sessionState.isGamePaused());

            if (sessionState.isGamePaused()) {
                soundManager.stopClip("background");
            } else {
                soundManager.playBackgroundMusic();
            }
        }

    }

    public void stopGame() {
        synchronized (stateLock) {
            sessionState.setGameRunning(false);
            soundManager.stopClip("background");
        }
        stopGameThread();
    }

    private void startGameThread() {
        if (gameLoopRunning) {
            return;
        }

        loopMetrics.reset();
        gameLoopRunning = true;
        gameLoopThread = new Thread(() -> {
            while (gameLoopRunning) {
                try {
                    onGameTick();
                } catch (RuntimeException ex) {
                    System.err.println("Game loop crashed:");
                    ex.printStackTrace();
                    gameLoopRunning = false;
                    break;
                }
                try {
                    Thread.sleep(TIMER_POLL_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            gameLoopRunning = false;
        }, "GameLoopThread");
        gameLoopThread.setDaemon(true);
        gameLoopThread.start();
    }

    private void stopGameThread() {
        gameLoopRunning = false;
        if (gameLoopThread != null) {
            gameLoopThread.interrupt();
            if (Thread.currentThread() != gameLoopThread) {
                try {
                    gameLoopThread.join(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            gameLoopThread = null;
        }
    }

    private void onGameTick() {
        synchronized (stateLock) {
            loopMetrics.beginTimerTick();

            if (sessionState.isGameRunning() && !sessionState.isGamePaused()) {
                int updatesProcessed = 0;

                while (loopMetrics.getAccumulatedFrameNanos() >= TARGET_FRAME_NANOS && updatesProcessed < MAX_UPDATES_PER_TICK) {
                    long deltaTimeMs = Math.max(1L, Math.round(TARGET_FRAME_NANOS / 1_000_000.0));
                    long stageStartedAt = System.nanoTime();
                    updatePlayer(deltaTimeMs);
                    loopMetrics.recordPlayerUpdate(System.nanoTime() - stageStartedAt);

                    stageStartedAt = System.nanoTime();
                    combatSystem.updateEnemies(deltaTimeMs, world.getPlayer());
                    loopMetrics.recordEnemyUpdate(System.nanoTime() - stageStartedAt);

                    stageStartedAt = System.nanoTime();
                    combatSystem.updateProjectiles(deltaTimeMs, world.getPlayer());
                    loopMetrics.recordProjectileUpdate(System.nanoTime() - stageStartedAt);

                    stageStartedAt = System.nanoTime();
                    world.updateWorldAnimations();
                    world.updateScreenPositions();
                    loopMetrics.recordAnimationUpdate(System.nanoTime() - stageStartedAt);

                    stageStartedAt = System.nanoTime();
                    combatSystem.checkCollisions(
                        world.getPlayer(),
                        world.getPlayerData(),
                        this::queueLevelUpChoices,
                        () -> triggerGameOver(false)
                    );
                    loopMetrics.recordCollisionUpdate(System.nanoTime() - stageStartedAt);

                    stageStartedAt = System.nanoTime();
                    updateEffects();
                    loopMetrics.recordEffectsUpdate(System.nanoTime() - stageStartedAt);

                    loopMetrics.consumeFrame(TARGET_FRAME_NANOS);
                    updatesProcessed++;
                }

                if (updatesProcessed == MAX_UPDATES_PER_TICK && loopMetrics.getAccumulatedFrameNanos() > TARGET_FRAME_NANOS) {
                    loopMetrics.clampAccumulatedFrameNanos(TARGET_FRAME_NANOS);
                }
            }

            levelUpManager.processPendingChoices(sessionState, world, inputState, soundManager);

            if (sessionState.isGameOver() && sessionState.getGameOverTime() > 0) {
                long elapsed = System.currentTimeMillis() - sessionState.getGameOverTime();
                if (elapsed >= GAME_OVER_EXIT_DELAY) {
                    System.exit(0);
                }
            }

            loopMetrics.logProfilerIfReady(sessionState, world);
        }
    }

    public void triggerGameOver(boolean won) {
        synchronized (stateLock) {
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
        }
    }

    public void updatePlayer(long deltaTime) {
        PlayerSprite player = world.getPlayer();
        Player playerData = world.getPlayerData();
        if (player == null || !sessionState.isGameRunning() || sessionState.isGamePaused()) {
            return;
        }

        player.updateSpeedBoost(deltaTime);
        player.updateDamageFlash(deltaTime);
        if (playerData != null) {
            playerData.updateRegeneration(deltaTime);
        }

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
            player.move(moveDirection, deltaTime);

            if (!playerCollisionResolver.resolve(player, world.getSolidObjects(), oldWorldX, oldWorldY, moveDirection)) {
                player.setWorldX(oldWorldX);
                player.setWorldY(oldWorldY);
            }
        }

        if (!inputState.isAnyMovementPressed()) {
            player.setIdle();
        }

        player.update(deltaTime);
        updatePlayerWeapon(deltaTime, player, playerData);
        world.updateCamera(viewportWidth, viewportHeight);
    }

    private void updatePlayerWeapon(long deltaTime, PlayerSprite player, Player playerData) {
        if (playerData == null || playerData.getWeapon() == null) {
            return;
        }
        playerData.getWeapon().update(deltaTime, world, player, playerData);
    }

    public void updateEffects() {
        for (ImageFX effect : effects) {
            effect.update();
        }

        if (screenGrayScaleFX != null) {
            screenGrayScaleFX.update();
        }

        loopMetrics.updateHudIfReady(infoPanel, world.getPlayerData(), sessionState);
    }

    public void renderToScreen(Graphics2D targetGraphics, int renderWidth, int renderHeight) {
        synchronized (stateLock) {
            if (sessionState.isGameOver() && sessionState.getGameOverTime() > 0 && !sessionState.isGameExiting()) {
                long elapsed = System.currentTimeMillis() - sessionState.getGameOverTime();
                if (elapsed >= GAME_OVER_EXIT_DELAY) {
                    sessionState.setGameExiting(true);
                    System.exit(0);
                }
            }

            setViewportSize(renderWidth, renderHeight);
            ensureRenderBuffer(renderWidth, renderHeight);

            long drawStartedAt = System.nanoTime();
            drawToBuffer(doubleBufferG2, renderWidth, renderHeight);
            loopMetrics.recordDraw(System.nanoTime() - drawStartedAt);
            targetGraphics.drawImage(doubleBufferImage, 0, 0, renderWidth, renderHeight, null);
            loopMetrics.updateRenderedFps(sessionState);
        }
    }

    private void drawToBuffer(Graphics2D g2, int renderWidth, int renderHeight) {
        renderer.draw(g2, renderWidth, renderHeight, world, sessionState, effects, screenGrayScaleFX, doubleBufferImage);
    }

    private void ensureRenderBuffer(int width, int height) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        if (doubleBufferImage != null
            && doubleBufferImage.getWidth() == safeWidth
            && doubleBufferImage.getHeight() == safeHeight) {
            return;
        }

        if (doubleBufferG2 != null) {
            doubleBufferG2.dispose();
        }

        doubleBufferImage = new BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_ARGB);
        doubleBufferG2 = doubleBufferImage.createGraphics();
    }

    public void setViewportSize(int viewportWidth, int viewportHeight) {
        this.viewportWidth = Math.max(1, viewportWidth);
        this.viewportHeight = Math.max(1, viewportHeight);
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

    public Player getPlayerData() {
        return world.getPlayerData();
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

    public void setSelectedWeapon(WeaponType selectedWeapon) {
        synchronized (stateLock) {
            if (selectedWeapon == null) {
                return;
            }

            this.selectedWeapon = selectedWeapon;
            if (world.getPlayerData() != null) {
                world.getPlayerData().getWeapon().onUnequipped(world);
                world.getPlayerData().setWeaponType(selectedWeapon);
            }
        }
    }

    public void setSelectedEnemyType(EnemySpawnType selectedEnemyType) {
        synchronized (stateLock) {
            world.getEnemySpawner().setSelectedEnemyType(selectedEnemyType);
        }
    }

    public EnemySpawnType getSelectedEnemyType() {
        return world.getEnemySpawner().getSelectedEnemyType();
    }

    public boolean isLevelUpChoiceActive() {
        return levelUpManager.isChoiceActive();
    }

    public String getLevelUpPromptMessage() {
        return levelUpManager.getPromptMessage(world.getPlayerData());
    }

    public List<PlayerUpgradeOption> getLevelUpChoices() {
        return levelUpManager.getCurrentChoices();
    }

    public void applyLevelUpChoice(int selectedIndex) {
        synchronized (stateLock) {
            levelUpManager.applyChoice(selectedIndex, sessionState, world, inputState, soundManager, infoPanel);
        }
    }

    private void queueLevelUpChoices(int levelsGained) {
        levelUpManager.queueChoices(levelsGained, sessionState);
    }

}

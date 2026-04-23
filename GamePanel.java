import javax.swing.JPanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel {
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private static final int WORLD_WIDTH = 3000;
    private static final int WORLD_HEIGHT = 3000;
    private static final int TOTAL_COLLECTIBLES = 3;
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
    private Runnable renderCallback;
    private WeaponType selectedWeapon;
    private int viewportWidth;
    private int viewportHeight;

    // Map-transition fade
    private int fadeAlpha = 0;
    private boolean fading = false;
    private boolean fadingOut = true;
    private int pendingLevel = -1;

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
                world, sessionState, SoundManager.getInstance(),
                GOLDEN_TINT_DURATION, BAT_FIRE_INTERVAL_MS,
                EXPERIENCE_PER_COLLECTIBLE,
                BAT_STOP_DISTANCE, BAT_BULLET_SPEED);
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

        gameLoopThread = null;
        gameLoopRunning = false;
        renderCallback = null;
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
            if (sessionState.isGameRunning())
                return;

            sessionState.resetForNewGame();
            screenGrayScaleFX = null;
            createGameEntities();
            loopMetrics.reset();
            levelUpManager.reset();
            sessionState.setFps(0);
            if (world.getPlayerData() != null) {
                world.getPlayerData().setWeaponType(selectedWeapon);
            }
            soundManager.playBackgroundMusic(world.getTerrainForCurrentLevel());
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
                soundManager.stopClip("grass-background");
                soundManager.stopClip("sand-background");
                soundManager.stopClip("snow-background");
            } else
                soundManager.playBackgroundMusic(world.getTerrainForCurrentLevel());
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
        if (gameLoopRunning)
            return;

        loopMetrics.reset();
        gameLoopRunning = true;
        gameLoopThread = new Thread(() -> {
            while (gameLoopRunning) {
                try {
                    onGameTick();
                    requestRender();
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

                while (loopMetrics.getAccumulatedFrameNanos() >= TARGET_FRAME_NANOS
                        && updatesProcessed < MAX_UPDATES_PER_TICK) {

                    long deltaTimeMs = Math.max(1L,
                            Math.round(TARGET_FRAME_NANOS / 1_000_000.0));

                    long t = System.nanoTime();
                    updatePlayer(deltaTimeMs);
                    loopMetrics.recordPlayerUpdate(System.nanoTime() - t);

                    int playerLevel = world.getPlayerData() != null
                            ? world.getPlayerData().getLevel()
                            : 1;
                    world.updateBossSystem(deltaTimeMs, playerLevel);

                    t = System.nanoTime();
                    combatSystem.updateEnemies(deltaTimeMs, world.getPlayer());
                    loopMetrics.recordEnemyUpdate(System.nanoTime() - t);

                    t = System.nanoTime();
                    combatSystem.updateProjectiles(deltaTimeMs, world.getPlayer());
                    loopMetrics.recordProjectileUpdate(System.nanoTime() - t);

                    t = System.nanoTime();
                    world.updateWorldAnimations();
                    world.updateScreenPositions();
                    loopMetrics.recordAnimationUpdate(System.nanoTime() - t);

                    t = System.nanoTime();
                    combatSystem.checkCollisions(
                            world.getPlayer(), world.getPlayerData(),
                            this::queueLevelUpChoices,
                            () -> triggerGameOver(false));
                    loopMetrics.recordCollisionUpdate(System.nanoTime() - t);

                    t = System.nanoTime();
                    updateEffects();
                    loopMetrics.recordEffectsUpdate(System.nanoTime() - t);

                    loopMetrics.consumeFrame(TARGET_FRAME_NANOS);
                    updatesProcessed++;
                }

                if (updatesProcessed == MAX_UPDATES_PER_TICK
                        && loopMetrics.getAccumulatedFrameNanos() > TARGET_FRAME_NANOS) {
                    loopMetrics.clampAccumulatedFrameNanos(TARGET_FRAME_NANOS);
                }
            }

            levelUpManager.processPendingChoices(sessionState, world, inputState, soundManager);

            if (sessionState.isGameOver() && sessionState.getGameOverTime() > 0) {
                long elapsed = System.currentTimeMillis() - sessionState.getGameOverTime();
                if (elapsed >= GAME_OVER_EXIT_DELAY)
                    System.exit(0);
            }

            loopMetrics.logProfilerIfReady(sessionState, world);
        }
    }

    private void requestRender() {
        Runnable cb = renderCallback;
        if (cb != null)
            cb.run();
    }

    public void updatePlayer(long deltaTime) {
        PlayerSprite player = world.getPlayer();
        Player playerData = world.getPlayerData();
        if (player == null || !sessionState.isGameRunning() || sessionState.isGamePaused())
            return;

        player.updateSpeedBoost(deltaTime);
        player.updateDamageFlash(deltaTime);
        if (playerData != null)
            playerData.updateRegeneration(deltaTime);

        if (sessionState.isGoldenTintActive()) {
            long remaining = sessionState.getGoldenTintTimer() - deltaTime;
            sessionState.setGoldenTintTimer(remaining);
            if (remaining <= 0) {
                sessionState.setGoldenTintActive(false);
                sessionState.setGoldenTintTimer(0);
            }
        }

        int oldWorldX = player.getWorldX();
        int oldWorldY = player.getWorldY();
        int moveDirection = inputState.resolveMovementDirection();

        LevelPortal portal = world.getLevelPortal();
        if (portal != null)
            portal.update();

        if (moveDirection != 0) {
            player.move(moveDirection, deltaTime, world.getTileMap());

            if (!playerCollisionResolver.resolve(player, world.getSolidObjects(),
                    oldWorldX, oldWorldY, moveDirection, world.getTileMap())) {
                player.setWorldX(oldWorldX);
                player.setWorldY(oldWorldY);
            }

            if (portal != null && portal.collidesWithPlayer(player.getBoundingRectangle())) {
                player.setWorldX(oldWorldX);
                player.setWorldY(oldWorldY);

                int currentPlayerLevel = playerData != null ? playerData.getLevel() : 1;
                if (portal.isBossDead(currentPlayerLevel)) {
                    soundManager.playClip("portal-sound", false);
                    triggerLevelTransition(world.getNextLevel());
                }
            }
        }

        if (!inputState.isAnyMovementPressed())
            player.setIdle();

        player.update(deltaTime);
        updatePlayerWeapon(deltaTime, player, playerData);
        world.updateCamera(viewportWidth, viewportHeight);
    }

    private void updatePlayerWeapon(long deltaTime, PlayerSprite player, Player playerData) {
        if (playerData == null || playerData.getWeapon() == null)
            return;
        playerData.getWeapon().update(deltaTime, world, player, playerData);
    }

    public int getCurrentLevel() {
        return world.getCurrentLevel();
    }

    public void triggerLevelTransition(int nextLevel) {
        if (!fading) {
            pendingLevel = nextLevel;
            fading = true;
            fadingOut = true;
            fadeAlpha = 0;
        }
    }

    public void updateEffects() {
        for (ImageFX effect : effects)
            effect.update();
        if (screenGrayScaleFX != null)
            screenGrayScaleFX.update();
        loopMetrics.updateHudIfReady(infoPanel, world.getPlayerData(), sessionState);
        updateFade();
    }

    public void updateFade() {
        if (!fading)
            return;

        if (fadingOut) {
            fadeAlpha = Math.min(255, fadeAlpha + 5);
            if (fadeAlpha >= 255) {
                world.loadLevel(pendingLevel);
                soundManager.playBackgroundMusic(world.getTerrainForCurrentLevel());
                fadingOut = false;
            }
        } else {
            fadeAlpha = Math.max(0, fadeAlpha - 5);
            if (fadeAlpha <= 0) {
                fading = false;
                pendingLevel = -1;
            }
        }
    }

    public void triggerGameOver(boolean won) {
        synchronized (stateLock) {
            sessionState.setGameOver(true);
            sessionState.setGameRunning(false);
            soundManager.stopAll();
            sessionState.setGameOverTime(System.currentTimeMillis());
            sessionState.setGameExiting(false);

            if (won && world.getBackgroundImage() != null) {
                BufferedImage bg = world.getBackgroundImage();
                BufferedImage grayBg = new BufferedImage(bg.getWidth(), bg.getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = grayBg.createGraphics();
                g2.drawImage(bg, 0, 0, null);
                g2.dispose();
                int[] pixels = new int[grayBg.getWidth() * grayBg.getHeight()];
                grayBg.getRGB(0, 0, grayBg.getWidth(), grayBg.getHeight(),
                        pixels, 0, grayBg.getWidth());
                for (int i = 0; i < pixels.length; i++) {
                    int a = (pixels[i] >> 24) & 255;
                    int r = (pixels[i] >> 16) & 255;
                    int g = (pixels[i] >> 8) & 255;
                    int b = pixels[i] & 255;
                    int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                    pixels[i] = (a << 24) | (gray << 16) | (gray << 8) | gray;
                }
                grayBg.setRGB(0, 0, grayBg.getWidth(), grayBg.getHeight(),
                        pixels, 0, grayBg.getWidth());
                screenGrayScaleFX = new GrayScaleFX(0, 0,
                        WORLD_WIDTH, WORLD_HEIGHT, bg, grayBg);
            }
        }
    }

    public void renderToScreen(Graphics2D targetGraphics,
            int renderWidth, int renderHeight) {
        synchronized (stateLock) {
            if (sessionState.isGameOver() && sessionState.getGameOverTime() > 0
                    && !sessionState.isGameExiting()) {
                long elapsed = System.currentTimeMillis() - sessionState.getGameOverTime();
                if (elapsed >= GAME_OVER_EXIT_DELAY) {
                    sessionState.setGameExiting(true);
                    System.exit(0);
                }
            }

            setViewportSize(renderWidth, renderHeight);
            ensureRenderBuffer(renderWidth, renderHeight);

            long drawStarted = System.nanoTime();
            drawToBuffer(doubleBufferG2, renderWidth, renderHeight);
            loopMetrics.recordDraw(System.nanoTime() - drawStarted);
            targetGraphics.drawImage(doubleBufferImage, 0, 0, renderWidth, renderHeight, null);
            loopMetrics.updateRenderedFps(sessionState);
        }
    }

    private void drawToBuffer(Graphics2D g2, int renderWidth, int renderHeight) {
        renderer.draw(g2, renderWidth, renderHeight, world, sessionState, effects,
                screenGrayScaleFX, doubleBufferImage, fadeAlpha);
    }

    private void ensureRenderBuffer(int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        if (doubleBufferImage != null
                && doubleBufferImage.getWidth() == w
                && doubleBufferImage.getHeight() == h)
            return;

        if (doubleBufferG2 != null)
            doubleBufferG2.dispose();
        doubleBufferImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        doubleBufferG2 = doubleBufferImage.createGraphics();
    }

    public void setViewportSize(int w, int h) {
        viewportWidth = Math.max(1, w);
        viewportHeight = Math.max(1, h);
    }

    public void setRenderCallback(Runnable cb) {
        this.renderCallback = cb;
    }

    public void startLoop() {
        startGameThread();
    }

    public void setLeftKeyPressed(boolean p) {
        inputState.setLeftPressed(p);
    }

    public void setRightKeyPressed(boolean p) {
        inputState.setRightPressed(p);
    }

    public void setUpKeyPressed(boolean p) {
        inputState.setUpPressed(p);
    }

    public void setDownKeyPressed(boolean p) {
        inputState.setDownPressed(p);
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

    public void setSelectedWeapon(WeaponType selectedWeapon) {
        synchronized (stateLock) {
            if (selectedWeapon == null)
                return;
            this.selectedWeapon = selectedWeapon;
            if (world.getPlayerData() != null) {
                world.getPlayerData().getWeapon().onUnequipped(world);
                world.getPlayerData().setWeaponType(selectedWeapon);
            }
        }
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

    public TestEnemySpawnType getActiveTestEnemySpawnType() {
        synchronized (stateLock) {
            return world.getEnemySpawner().getActiveSpawnType();
        }
    }

    public void activateTestEnemySpawn(TestEnemySpawnType spawnType) {
        synchronized (stateLock) {
            world.activateTestEnemySpawn(spawnType);
        }
    }

    public boolean spawnTestBoss(TestBossSpawnType bossType) {
        synchronized (stateLock) {
            return world.spawnTestBoss(bossType);
        }
    }

    private void queueLevelUpChoices(int levelsGained) {
        levelUpManager.queueChoices(levelsGained, sessionState);
    }
}
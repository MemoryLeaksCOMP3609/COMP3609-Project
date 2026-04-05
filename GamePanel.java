import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

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

    private BufferedImage doubleBufferImage;
    private Graphics2D doubleBufferG2;

    private ArrayList<ImageFX> effects;
    private GrayScaleFX screenGrayScaleFX;
    private SoundManager soundManager;
    private InfoPanel infoPanel;
    private Timer gameLoopTimer;
    private WeaponType selectedWeapon;

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

        gameLoopTimer = null;
        selectedWeapon = WeaponType.FIRE_ARROW;
    }

    public void createGameEntities() {
        world.initializeEntities(this, TOTAL_COLLECTIBLES);
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
        loopMetrics.reset();
        levelUpManager.reset();
        sessionState.setFps(0);
        if (world.getPlayerData() != null) {
            world.getPlayerData().setWeaponType(selectedWeapon);
        }
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
        if (gameLoopTimer != null && gameLoopTimer.isRunning()) {
            return;
        }

        loopMetrics.reset();
        gameLoopTimer = new Timer(TIMER_POLL_DELAY_MS, event -> onGameTick());
        gameLoopTimer.setCoalesce(false);
        gameLoopTimer.start();
    }

    private void stopGameThread() {
        if (gameLoopTimer != null) {
            gameLoopTimer.stop();
            gameLoopTimer = null;
        }
    }

    private void onGameTick() {
        loopMetrics.beginTimerTick();

        if (sessionState.isGameRunning() && !sessionState.isGamePaused()) {
            int updatesProcessed = 0;
            boolean advancedFrame = false;

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
                advancedFrame = true;
            }

            if (updatesProcessed == MAX_UPDATES_PER_TICK && loopMetrics.getAccumulatedFrameNanos() > TARGET_FRAME_NANOS) {
                loopMetrics.clampAccumulatedFrameNanos(TARGET_FRAME_NANOS);
            }

            if (advancedFrame) {
                repaint();
            }
        }

        levelUpManager.processPendingChoices(this, sessionState, world, inputState, soundManager, infoPanel);

        if (sessionState.isGameOver() && sessionState.getGameOverTime() > 0) {
            long elapsed = System.currentTimeMillis() - sessionState.getGameOverTime();
            if (elapsed >= GAME_OVER_EXIT_DELAY) {
                System.exit(0);
            }
        }

        loopMetrics.logProfilerIfReady(sessionState, world);
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
        world.updateCamera(getWidth(), getHeight());
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
            long drawStartedAt = System.nanoTime();
            drawToBuffer(doubleBufferG2);
            loopMetrics.recordDraw(System.nanoTime() - drawStartedAt);
            g.drawImage(doubleBufferImage, 0, 0, null);
        } else {
            long drawStartedAt = System.nanoTime();
            drawToBuffer((Graphics2D) g);
            loopMetrics.recordDraw(System.nanoTime() - drawStartedAt);
        }
        loopMetrics.updateRenderedFps(sessionState);
    }

    private void drawToBuffer(Graphics2D g2) {
        renderer.draw(g2, getWidth(), getHeight(), world, sessionState, effects, screenGrayScaleFX, doubleBufferImage);
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
        if (selectedWeapon == null) {
            return;
        }

        this.selectedWeapon = selectedWeapon;
        if (world.getPlayerData() != null) {
            world.getPlayerData().getWeapon().onUnequipped(world);
            world.getPlayerData().setWeaponType(selectedWeapon);
        }
    }

    public void setSelectedEnemyType(EnemySpawnType selectedEnemyType) {
        world.getEnemySpawner().setSelectedEnemyType(selectedEnemyType);
    }

    public EnemySpawnType getSelectedEnemyType() {
        return world.getEnemySpawner().getSelectedEnemyType();
    }

    private void queueLevelUpChoices(int levelsGained) {
        levelUpManager.queueChoices(levelsGained, sessionState);
    }

}

import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;

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

    private BufferedImage doubleBufferImage;
    private Graphics2D doubleBufferG2;

    private ArrayList<ImageFX> effects;
    private GrayScaleFX screenGrayScaleFX;
    private SoundManager soundManager;
    private InfoPanel infoPanel;
    private Timer gameLoopTimer;
    private WeaponType selectedWeapon;
    private long lastFrameTimeNanos;
    private long lastFpsSampleNanos;
    private long lastHudUpdateNanos;
    private long lastProfilerSampleNanos;
    private long accumulatedFrameNanos;
    private int renderedFramesSinceSample;
    private long totalPlayerUpdateNanos;
    private long totalEnemyUpdateNanos;
    private long totalProjectileUpdateNanos;
    private long totalAnimationUpdateNanos;
    private long totalCollisionNanos;
    private long totalEffectsUpdateNanos;
    private long totalDrawNanos;
    private int profiledUpdateCount;
    private int profiledDrawCount;
    private int pendingLevelUpChoices;
    private boolean levelUpDialogOpen;

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
        lastFrameTimeNanos = System.nanoTime();
        lastFpsSampleNanos = System.nanoTime();
        lastHudUpdateNanos = System.nanoTime();
        lastProfilerSampleNanos = System.nanoTime();
        accumulatedFrameNanos = 0L;
        renderedFramesSinceSample = 0;
        totalPlayerUpdateNanos = 0L;
        totalEnemyUpdateNanos = 0L;
        totalProjectileUpdateNanos = 0L;
        totalAnimationUpdateNanos = 0L;
        totalCollisionNanos = 0L;
        totalEffectsUpdateNanos = 0L;
        totalDrawNanos = 0L;
        profiledUpdateCount = 0;
        profiledDrawCount = 0;
        pendingLevelUpChoices = 0;
        levelUpDialogOpen = false;
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
        lastFrameTimeNanos = System.nanoTime();
        lastFpsSampleNanos = System.nanoTime();
        lastHudUpdateNanos = System.nanoTime();
        lastProfilerSampleNanos = System.nanoTime();
        accumulatedFrameNanos = 0L;
        renderedFramesSinceSample = 0;
        totalPlayerUpdateNanos = 0L;
        totalEnemyUpdateNanos = 0L;
        totalProjectileUpdateNanos = 0L;
        totalAnimationUpdateNanos = 0L;
        totalCollisionNanos = 0L;
        totalEffectsUpdateNanos = 0L;
        totalDrawNanos = 0L;
        profiledUpdateCount = 0;
        profiledDrawCount = 0;
        pendingLevelUpChoices = 0;
        levelUpDialogOpen = false;
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

        lastFrameTimeNanos = System.nanoTime();
        accumulatedFrameNanos = 0L;
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
        long currentTimeNanos = System.nanoTime();
        long elapsedNanos = currentTimeNanos - lastFrameTimeNanos;
        lastFrameTimeNanos = currentTimeNanos;
        accumulatedFrameNanos += elapsedNanos;

        if (sessionState.isGameRunning() && !sessionState.isGamePaused()) {
            int updatesProcessed = 0;
            boolean advancedFrame = false;

            while (accumulatedFrameNanos >= TARGET_FRAME_NANOS && updatesProcessed < MAX_UPDATES_PER_TICK) {
                long deltaTimeMs = Math.max(1L, Math.round(TARGET_FRAME_NANOS / 1_000_000.0));
                long stageStartedAt = System.nanoTime();
                updatePlayer(deltaTimeMs);
                totalPlayerUpdateNanos += System.nanoTime() - stageStartedAt;

                stageStartedAt = System.nanoTime();
                combatSystem.updateEnemies(deltaTimeMs, world.getPlayer());
                totalEnemyUpdateNanos += System.nanoTime() - stageStartedAt;

                stageStartedAt = System.nanoTime();
                combatSystem.updateProjectiles(deltaTimeMs, world.getPlayer());
                totalProjectileUpdateNanos += System.nanoTime() - stageStartedAt;

                stageStartedAt = System.nanoTime();
                world.updateWorldAnimations();
                world.updateScreenPositions();
                totalAnimationUpdateNanos += System.nanoTime() - stageStartedAt;

                stageStartedAt = System.nanoTime();
                combatSystem.checkCollisions(
                    world.getPlayer(),
                    world.getPlayerData(),
                    this::queueLevelUpChoices,
                    () -> triggerGameOver(false)
                );
                totalCollisionNanos += System.nanoTime() - stageStartedAt;

                stageStartedAt = System.nanoTime();
                updateEffects();
                totalEffectsUpdateNanos += System.nanoTime() - stageStartedAt;

                accumulatedFrameNanos -= TARGET_FRAME_NANOS;
                updatesProcessed++;
                advancedFrame = true;
                profiledUpdateCount++;
            }

            if (updatesProcessed == MAX_UPDATES_PER_TICK && accumulatedFrameNanos > TARGET_FRAME_NANOS) {
                accumulatedFrameNanos = TARGET_FRAME_NANOS;
            }

            if (advancedFrame) {
                repaint();
            }
        }

        processPendingLevelUpChoices();

        if (sessionState.isGameOver() && sessionState.getGameOverTime() > 0) {
            long elapsed = System.currentTimeMillis() - sessionState.getGameOverTime();
            if (elapsed >= GAME_OVER_EXIT_DELAY) {
                System.exit(0);
            }
        }

        logFrameProfilerIfReady();
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

        long now = System.nanoTime();
        if (infoPanel != null && now - lastHudUpdateNanos >= 100_000_000L) {
            infoPanel.updatePlayerStats(world.getPlayerData());
            infoPanel.updateFPS(sessionState.getFps());
            infoPanel.updateActiveEffects(sessionState.getActiveEffectName());
            lastHudUpdateNanos = now;
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
            long drawStartedAt = System.nanoTime();
            drawToBuffer(doubleBufferG2);
            totalDrawNanos += System.nanoTime() - drawStartedAt;
            profiledDrawCount++;
            g.drawImage(doubleBufferImage, 0, 0, null);
        } else {
            long drawStartedAt = System.nanoTime();
            drawToBuffer((Graphics2D) g);
            totalDrawNanos += System.nanoTime() - drawStartedAt;
            profiledDrawCount++;
        }
        updateRenderedFps();
    }

    private void drawToBuffer(Graphics2D g2) {
        renderer.draw(g2, getWidth(), getHeight(), world, sessionState, effects, screenGrayScaleFX, doubleBufferImage);
    }

    private void updateRenderedFps() {
        renderedFramesSinceSample++;
        long now = System.nanoTime();
        long elapsedNanos = now - lastFpsSampleNanos;
        if (elapsedNanos >= 1_000_000_000L) {
            int fps = (int) Math.round(renderedFramesSinceSample * (1_000_000_000.0 / elapsedNanos));
            sessionState.setFps(fps);
            renderedFramesSinceSample = 0;
            lastFpsSampleNanos = now;
        }
    }

    private void logFrameProfilerIfReady() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastProfilerSampleNanos;
        if (elapsedNanos < 1_000_000_000L) {
            return;
        }

        double updateCount = Math.max(1, profiledUpdateCount);
        double drawCount = Math.max(1, profiledDrawCount);
        double playerMs = totalPlayerUpdateNanos / 1_000_000.0 / updateCount;
        double enemiesMs = totalEnemyUpdateNanos / 1_000_000.0 / updateCount;
        double projectilesMs = totalProjectileUpdateNanos / 1_000_000.0 / updateCount;
        double animationsMs = totalAnimationUpdateNanos / 1_000_000.0 / updateCount;
        double collisionsMs = totalCollisionNanos / 1_000_000.0 / updateCount;
        double effectsMs = totalEffectsUpdateNanos / 1_000_000.0 / updateCount;
        double drawMs = totalDrawNanos / 1_000_000.0 / drawCount;

        System.out.printf(
            "Frame profile: fps=%d updates=%d draws=%d player=%.2fms enemies=%.2fms projectiles=%.2fms anim=%.2fms collisions=%.2fms effects=%.2fms draw=%.2fms enemiesAlive=%d projectilesLive=%d crystals=%d%n",
            sessionState.getFps(),
            profiledUpdateCount,
            profiledDrawCount,
            playerMs,
            enemiesMs,
            projectilesMs,
            animationsMs,
            collisionsMs,
            effectsMs,
            drawMs,
            world.getEnemies().size(),
            world.getProjectiles().size(),
            world.getDroppedCrystals().size()
        );

        lastProfilerSampleNanos = now;
        totalPlayerUpdateNanos = 0L;
        totalEnemyUpdateNanos = 0L;
        totalProjectileUpdateNanos = 0L;
        totalAnimationUpdateNanos = 0L;
        totalCollisionNanos = 0L;
        totalEffectsUpdateNanos = 0L;
        totalDrawNanos = 0L;
        profiledUpdateCount = 0;
        profiledDrawCount = 0;
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

    private void queueLevelUpChoices(int levelsGained) {
        if (levelsGained <= 0) {
            return;
        }

        pendingLevelUpChoices += levelsGained;
        sessionState.setActiveEffectName("Level Up");
    }

    private void processPendingLevelUpChoices() {
        if (pendingLevelUpChoices <= 0 || levelUpDialogOpen || !sessionState.isGameRunning() || sessionState.isGameOver()) {
            return;
        }

        Player playerData = world.getPlayerData();
        if (playerData == null) {
            pendingLevelUpChoices = 0;
            return;
        }

        levelUpDialogOpen = true;
        boolean wasPaused = sessionState.isGamePaused();
        sessionState.setGamePaused(true);
        soundManager.stopClip("background");

        try {
            while (pendingLevelUpChoices > 0 && sessionState.isGameRunning() && !sessionState.isGameOver()) {
                PlayerUpgradeOption selectedUpgrade = promptForLevelUpChoice(playerData);
                if (selectedUpgrade == null) {
                    continue;
                }

                selectedUpgrade.apply(playerData);
                pendingLevelUpChoices--;
                sessionState.setActiveEffectName(selectedUpgrade.getDisplayName());
                if (infoPanel != null) {
                    infoPanel.updatePlayerStats(playerData);
                }
            }
        } finally {
            levelUpDialogOpen = false;
            inputState.clearMovement();
            if (world.getPlayer() != null) {
                world.getPlayer().setIdle();
            }
            sessionState.setGamePaused(wasPaused);
            if (!sessionState.isGameOver() && sessionState.isGameRunning() && !sessionState.isGamePaused()) {
                soundManager.playBackgroundMusic();
            }
            requestFocusInWindow();
        }
    }

    private PlayerUpgradeOption promptForLevelUpChoice(Player playerData) {
        ArrayList<PlayerUpgradeOption> choices = new ArrayList<PlayerUpgradeOption>();
        for (PlayerUpgradeOption option : PlayerUpgradeOption.values()) {
            if (option.isAvailable(playerData)) {
                choices.add(option);
            }
        }
        Collections.shuffle(choices);

        int choiceCount = Math.min(3, choices.size());
        String[] labels = new String[choiceCount];
        for (int i = 0; i < choiceCount; i++) {
            labels[i] = choices.get(i).getDisplayName();
        }

        String title = "Level Up";
        String message = playerData.isMaxLevel()
            ? "You reached the max level."
            : "Level " + playerData.getLevel() + " - choose a stat to upgrade";

        while (true) {
            int selectedIndex = JOptionPane.showOptionDialog(
                SwingUtilities.getWindowAncestor(this),
                message,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                labels,
                labels[0]
            );

            if (selectedIndex >= 0 && selectedIndex < choiceCount) {
                return choices.get(selectedIndex);
            }
        }
    }

}

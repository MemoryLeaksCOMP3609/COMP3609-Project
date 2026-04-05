public class GameLoopMetrics {
    private static final long HUD_UPDATE_INTERVAL_NANOS = 100_000_000L;
    private static final long SAMPLE_INTERVAL_NANOS = 1_000_000_000L;

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

    public GameLoopMetrics() {
        reset();
    }

    public void reset() {
        long now = System.nanoTime();
        lastFrameTimeNanos = now;
        lastFpsSampleNanos = now;
        lastHudUpdateNanos = now;
        lastProfilerSampleNanos = now;
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
    }

    public void beginTimerTick() {
        long currentTimeNanos = System.nanoTime();
        long elapsedNanos = currentTimeNanos - lastFrameTimeNanos;
        lastFrameTimeNanos = currentTimeNanos;
        accumulatedFrameNanos += elapsedNanos;
    }

    public long getAccumulatedFrameNanos() {
        return accumulatedFrameNanos;
    }

    public void consumeFrame(long frameNanos) {
        accumulatedFrameNanos -= frameNanos;
        profiledUpdateCount++;
    }

    public void clampAccumulatedFrameNanos(long frameNanos) {
        accumulatedFrameNanos = frameNanos;
    }

    public void recordPlayerUpdate(long nanos) {
        totalPlayerUpdateNanos += nanos;
    }

    public void recordEnemyUpdate(long nanos) {
        totalEnemyUpdateNanos += nanos;
    }

    public void recordProjectileUpdate(long nanos) {
        totalProjectileUpdateNanos += nanos;
    }

    public void recordAnimationUpdate(long nanos) {
        totalAnimationUpdateNanos += nanos;
    }

    public void recordCollisionUpdate(long nanos) {
        totalCollisionNanos += nanos;
    }

    public void recordEffectsUpdate(long nanos) {
        totalEffectsUpdateNanos += nanos;
    }

    public void recordDraw(long nanos) {
        totalDrawNanos += nanos;
        profiledDrawCount++;
    }

    public void updateRenderedFps(GameSessionState sessionState) {
        renderedFramesSinceSample++;
        long now = System.nanoTime();
        long elapsedNanos = now - lastFpsSampleNanos;
        if (elapsedNanos >= SAMPLE_INTERVAL_NANOS) {
            int fps = (int) Math.round(renderedFramesSinceSample * (SAMPLE_INTERVAL_NANOS * 1.0 / elapsedNanos));
            sessionState.setFps(fps);
            renderedFramesSinceSample = 0;
            lastFpsSampleNanos = now;
        }
    }

    public void updateHudIfReady(InfoPanel infoPanel, Player playerData, GameSessionState sessionState) {
        long now = System.nanoTime();
        if (infoPanel != null && now - lastHudUpdateNanos >= HUD_UPDATE_INTERVAL_NANOS) {
            infoPanel.updatePlayerStats(playerData);
            infoPanel.updateFPS(sessionState.getFps());
            lastHudUpdateNanos = now;
        }
    }

    public void logProfilerIfReady(GameSessionState sessionState, GameWorld world) {
        long now = System.nanoTime();
        long elapsedNanos = now - lastProfilerSampleNanos;
        if (elapsedNanos < SAMPLE_INTERVAL_NANOS) {
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
}

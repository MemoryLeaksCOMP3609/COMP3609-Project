public class BatEnemy extends Enemy {
    private static final long FRAME_DURATION = 120;
    private static final int BAT_FRAME_COUNT = 4;
    private static final long[] DEATH_FRAME_DURATIONS_MS = {500L, 500L, 500L, 2000L};

    public BatEnemy(int startX, int startY) {
        super("Bat", 20, 4, 5, 10, 10, startX, startY);
        renderScale = 0.25;
        moveAnimation = loadStripAnimation("images/enemies/bat/batMove.png", FRAME_DURATION, true, BAT_FRAME_COUNT);
        deathAnimation = buildAnimation(loadStripFrames("images/enemies/bat/batDeath.png", BAT_FRAME_COUNT),
            DEATH_FRAME_DURATIONS_MS, false);
        idleAnimation = moveAnimation;
        setAnimationForState(EnemyState.MOVING);
    }
}

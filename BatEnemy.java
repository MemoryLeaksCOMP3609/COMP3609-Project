public class BatEnemy extends Enemy {
    private static final long FRAME_DURATION = 120;
    private static final int BAT_FRAME_COUNT = 4;

    public BatEnemy(int startX, int startY) {
        super("Bat", 20, 4, 5, 10, 10, startX, startY);
        renderScale = 0.25;
        moveAnimation = loadStripAnimation("images/enemies/bat/batMove.png", FRAME_DURATION, true, BAT_FRAME_COUNT);
        deathAnimation = loadStripAnimation("images/enemies/bat/batDeath.png", FRAME_DURATION, false, BAT_FRAME_COUNT);
        idleAnimation = moveAnimation;
        setAnimationForState(EnemyState.MOVING);
    }
}

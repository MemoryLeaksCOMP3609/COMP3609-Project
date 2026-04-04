public class BatEnemy extends Enemy {
    private static final long FRAME_DURATION = 120;

    public BatEnemy(int startX, int startY) {
        super("Bat", 20, 4, 5, 10, 10, startX, startY);
        moveAnimation = loadStripAnimation("images/enemies/bat/batMove.png", FRAME_DURATION, true);
        deathAnimation = loadStripAnimation("images/enemies/bat/batDeath.png", FRAME_DURATION, false);
        idleAnimation = moveAnimation;
        setAnimationForState(EnemyState.MOVING);
    }
}

public class DarkGhostEnemy extends GhostEnemy {
    private static final long FRAME_DURATION = 120;

    public DarkGhostEnemy(int startX, int startY) {
        super("Dark Ghost", 45, 4, 12, 35, 35, 0.55, startX, startY);
        renderScale = 0.5;
        moveAnimation = loadStripAnimation("images/enemies/ghost/ghostDMove.png", FRAME_DURATION, true);
        deathAnimation = loadStripAnimation("images/enemies/ghost/ghostDDeath.png", FRAME_DURATION, false);
        idleAnimation = moveAnimation;
        setAnimationForState(EnemyState.MOVING);
    }
}

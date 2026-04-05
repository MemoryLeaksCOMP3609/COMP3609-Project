public class FrostGhostEnemy extends GhostEnemy {
    private static final long FRAME_DURATION = 120;

    public FrostGhostEnemy(int startX, int startY) {
        super("Frost Ghost", 36, 3, 10, 25, 25, 0.6, startX, startY);
        moveAnimation = loadStripAnimation("images/enemies/ghost/ghostFMove.png", FRAME_DURATION, true);
        deathAnimation = loadStripAnimation("images/enemies/ghost/ghostFDeath.png", FRAME_DURATION, false);
        idleAnimation = moveAnimation;
        setAnimationForState(EnemyState.MOVING);
    }
}

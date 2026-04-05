public class NormalGhostEnemy extends GhostEnemy {
    private static final long FRAME_DURATION = 120;

    public NormalGhostEnemy(int startX, int startY) {
        super("Ghost", 30, 3, 8, 20, 20, 0.65, startX, startY);
        renderScale = 0.5;
        moveAnimation = loadStripAnimation("images/enemies/ghost/ghostNMove.png", FRAME_DURATION, true);
        deathAnimation = loadStripAnimation("images/enemies/ghost/ghostNDeath.png", FRAME_DURATION, false);
        idleAnimation = moveAnimation;
        setAnimationForState(EnemyState.MOVING);
    }
}

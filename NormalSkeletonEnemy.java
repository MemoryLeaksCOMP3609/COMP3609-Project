public class NormalSkeletonEnemy extends SkeletonEnemy {
    private static final long FRAME_DURATION = 120;

    public NormalSkeletonEnemy(int startX, int startY) {
        super("Skeleton", 40, 2, 10, 20, 20, startX, startY);
        moveAnimation = loadStripAnimation("images/enemies/skeleton/skeleNMove.png", FRAME_DURATION, true);
        attackAnimation = loadStripAnimation("images/enemies/skeleton/skeleNAttack.png", FRAME_DURATION, true);
        deathAnimation = loadStripAnimation("images/enemies/skeleton/skeleNDeath.png", FRAME_DURATION, false);
        idleAnimation = moveAnimation;
        setAnimationForState(EnemyState.MOVING);
    }
}

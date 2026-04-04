public class ToxicSkeletonEnemy extends SkeletonEnemy {
    private static final long FRAME_DURATION = 120;

    public ToxicSkeletonEnemy(int startX, int startY) {
        super("Toxic Skeleton", 55, 3, 14, 30, 30, startX, startY);
        moveAnimation = loadStripAnimation("images/enemies/skeleton/skeleTMove.png", FRAME_DURATION, true);
        attackAnimation = loadStripAnimation("images/enemies/skeleton/skeleTAttack.png", FRAME_DURATION, true);
        deathAnimation = loadStripAnimation("images/enemies/skeleton/skeleTDeath.png", FRAME_DURATION, false);
        idleAnimation = moveAnimation;
        setAnimationForState(EnemyState.MOVING);
    }
}

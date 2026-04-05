public class ToxicSkeletonEnemy extends SkeletonEnemy {
    private static final long FRAME_DURATION = 120;

    public ToxicSkeletonEnemy(int startX, int startY) {
        super("Toxic Skeleton", 55, 7, 5, 30, 30, startX, startY);
        renderScale = 0.5;
        loadSkeletonAnimations(
            "images/enemies/skeleton/skeleTMove.png",
            "images/enemies/skeleton/skeleTAttack.png",
            "images/enemies/skeleton/skeleTDeath.png",
            FRAME_DURATION
        );
    }
}

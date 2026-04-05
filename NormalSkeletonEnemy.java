public class NormalSkeletonEnemy extends SkeletonEnemy {
    private static final long FRAME_DURATION = 120;

    public NormalSkeletonEnemy(int startX, int startY) {
        super("Skeleton", 40, 4, 5, 20, 20, startX, startY);
        renderScale = 0.5;
        loadSkeletonAnimations(
            "images/enemies/skeleton/skeleNMove.png",
            "images/enemies/skeleton/skeleNAttack.png",
            "images/enemies/skeleton/skeleNDeath.png",
            FRAME_DURATION
        );
    }
}

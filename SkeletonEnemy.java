public abstract class SkeletonEnemy extends Enemy {
    protected SkeletonEnemy(String name, int maxHealth, int movementSpeed, int contactDamage,
                            int scoreValue, int experienceReward, int startX, int startY) {
        super(name, maxHealth, movementSpeed, contactDamage, scoreValue, experienceReward, startX, startY);
    }
}

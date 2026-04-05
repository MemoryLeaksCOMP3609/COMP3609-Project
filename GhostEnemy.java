public abstract class GhostEnemy extends Enemy {
    protected final double transparency;

    protected GhostEnemy(String name, int maxHealth, int movementSpeed, int contactDamage,
                         int scoreValue, int experienceReward, double transparency,
                         int startX, int startY) {
        super(name, maxHealth, movementSpeed, contactDamage, scoreValue, experienceReward, startX, startY);
        this.transparency = transparency;
    }

    public double getTransparency() {
        return transparency;
    }
}

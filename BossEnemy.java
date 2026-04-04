public abstract class BossEnemy extends Enemy {
    protected final int phaseNumber;

    protected BossEnemy(String name, int phaseNumber, int maxHealth, int movementSpeed,
                        int contactDamage, int scoreValue, int experienceReward, int startX, int startY) {
        super(name, maxHealth, movementSpeed, contactDamage, scoreValue, experienceReward, startX, startY);
        this.phaseNumber = phaseNumber;
    }

    public int getPhaseNumber() {
        return phaseNumber;
    }

    public abstract void useSpecialAttack();
}

public class Player {
    private static final int BASE_MAX_HEALTH = 100;
    private static final int BASE_MOVE_SPEED = 5;
    private static final double BASE_DAMAGE_MULTIPLIER = 1.0;
    private static final double BASE_FIRE_RATE_MULTIPLIER = 1.0;
    private static final int BASE_LEVEL = 1;
    private static final int BASE_EXPERIENCE_TO_NEXT_LEVEL = 100;
    private static final int EXPERIENCE_STEP_PER_LEVEL = 25;

    private int maxHealth;
    private int health;
    private int moveSpeed;
    private double damageMultiplier;
    private double fireRateMultiplier;
    private int experience;
    private int level;
    private int experienceToNextLevel;

    public Player() {
        maxHealth = BASE_MAX_HEALTH;
        health = maxHealth;
        moveSpeed = BASE_MOVE_SPEED;
        damageMultiplier = BASE_DAMAGE_MULTIPLIER;
        fireRateMultiplier = BASE_FIRE_RATE_MULTIPLIER;
        experience = 0;
        level = BASE_LEVEL;
        experienceToNextLevel = BASE_EXPERIENCE_TO_NEXT_LEVEL;
    }

    public void heal(int amount) {
        if (amount <= 0) {
            return;
        }
        health = Math.min(maxHealth, health + amount);
    }

    public void takeDamage(int amount) {
        if (amount <= 0) {
            return;
        }
        health = Math.max(0, health - amount);
    }

    public boolean gainExperience(int amount) {
        if (amount <= 0) {
            return false;
        }

        experience += amount;
        boolean leveledUp = false;

        while (experience >= experienceToNextLevel) {
            experience -= experienceToNextLevel;
            level++;
            experienceToNextLevel += EXPERIENCE_STEP_PER_LEVEL;
            leveledUp = true;
        }

        return leveledUp;
    }

    public void increaseMoveSpeed(int amount) {
        if (amount > 0) {
            moveSpeed += amount;
        }
    }

    public void increaseMaxHealth(int amount) {
        if (amount > 0) {
            maxHealth += amount;
            health += amount;
        }
    }

    public void increaseDamageMultiplier(double amount) {
        if (amount > 0) {
            damageMultiplier += amount;
        }
    }

    public void increaseFireRateMultiplier(double amount) {
        if (amount > 0) {
            fireRateMultiplier += amount;
        }
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getHealth() {
        return health;
    }

    public int getMoveSpeed() {
        return moveSpeed;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public double getFireRateMultiplier() {
        return fireRateMultiplier;
    }

    public int getExperience() {
        return experience;
    }

    public int getLevel() {
        return level;
    }

    public int getExperienceToNextLevel() {
        return experienceToNextLevel;
    }
}

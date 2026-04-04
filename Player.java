public class Player {
    public static final int MAX_LEVEL = 20;

    private static final int BASE_MAX_HEALTH = 100;
    private static final int BASE_MOVE_SPEED = 5;
    private static final double BASE_DAMAGE_MULTIPLIER = 1.0;
    private static final double BASE_FIRE_RATE_MULTIPLIER = 1.0;
    private static final double BASE_PROJECTILE_COUNT_MULTIPLIER = 1.0;
    private static final double BASE_PROJECTILE_SIZE_MULTIPLIER = 1.0;
    private static final int BASE_HEALTH_REGEN_PER_INTERVAL = 1;
    private static final long HEALTH_REGEN_INTERVAL_MS = 5000L;
    private static final int BASE_LEVEL = 1;
    private static final int BASE_EXPERIENCE_TO_NEXT_LEVEL = 100;
    private static final double EXPERIENCE_GROWTH_RATE = 1.10;

    private int maxHealth;
    private int health;
    private int moveSpeed;
    private double damageMultiplier;
    private double fireRateMultiplier;
    private double projectileCountMultiplier;
    private double projectileSizeMultiplier;
    private int healthRegenPerInterval;
    private int experience;
    private int level;
    private int experienceToNextLevel;
    private WeaponType weaponType;
    private long healthRegenAccumulatorMs;

    public Player() {
        maxHealth = BASE_MAX_HEALTH;
        health = maxHealth;
        moveSpeed = BASE_MOVE_SPEED;
        damageMultiplier = BASE_DAMAGE_MULTIPLIER;
        fireRateMultiplier = BASE_FIRE_RATE_MULTIPLIER;
        projectileCountMultiplier = BASE_PROJECTILE_COUNT_MULTIPLIER;
        projectileSizeMultiplier = BASE_PROJECTILE_SIZE_MULTIPLIER;
        healthRegenPerInterval = BASE_HEALTH_REGEN_PER_INTERVAL;
        experience = 0;
        level = BASE_LEVEL;
        experienceToNextLevel = BASE_EXPERIENCE_TO_NEXT_LEVEL;
        weaponType = WeaponType.FIRE_ARROW;
        healthRegenAccumulatorMs = 0L;
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

    public int gainExperience(int amount) {
        if (amount <= 0 || level >= MAX_LEVEL) {
            if (level >= MAX_LEVEL) {
                experience = 0;
            }
            return 0;
        }

        experience += amount;
        int levelsGained = 0;

        while (level < MAX_LEVEL && experience >= experienceToNextLevel) {
            experience -= experienceToNextLevel;
            level++;
            levelsGained++;

            if (level >= MAX_LEVEL) {
                level = MAX_LEVEL;
                experience = 0;
                experienceToNextLevel = 0;
                break;
            }

            experienceToNextLevel = (int) Math.ceil(experienceToNextLevel * EXPERIENCE_GROWTH_RATE);
        }

        return levelsGained;
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

    public void increaseProjectileCountMultiplier(double amount) {
        if (amount > 0) {
            projectileCountMultiplier += amount;
        }
    }

    public void increaseProjectileSizeMultiplier(double amount) {
        if (amount > 0) {
            projectileSizeMultiplier += amount;
        }
    }

    public void increaseHealthRegenPerInterval(int amount) {
        if (amount > 0) {
            healthRegenPerInterval += amount;
        }
    }

    public void updateRegeneration(long deltaTimeMs) {
        if (deltaTimeMs <= 0 || health <= 0 || health >= maxHealth || healthRegenPerInterval <= 0) {
            if (health >= maxHealth) {
                healthRegenAccumulatorMs = 0L;
            }
            return;
        }

        healthRegenAccumulatorMs += deltaTimeMs;
        while (healthRegenAccumulatorMs >= HEALTH_REGEN_INTERVAL_MS && health < maxHealth) {
            heal(healthRegenPerInterval);
            healthRegenAccumulatorMs -= HEALTH_REGEN_INTERVAL_MS;
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

    public double getProjectileCountMultiplier() {
        return projectileCountMultiplier;
    }

    public double getProjectileSizeMultiplier() {
        return projectileSizeMultiplier;
    }

    public int getHealthRegenPerInterval() {
        return healthRegenPerInterval;
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

    public boolean isMaxLevel() {
        return level >= MAX_LEVEL;
    }

    public WeaponType getWeaponType() {
        return weaponType;
    }

    public void setWeaponType(WeaponType weaponType) {
        if (weaponType != null) {
            this.weaponType = weaponType;
        }
    }
}

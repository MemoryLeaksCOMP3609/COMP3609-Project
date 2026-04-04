public abstract class Weapon {
    protected static final int PLAYER_ATTACK_RANGE = 500;
    protected static final int PLAYER_BASE_DAMAGE = 10;
    protected static final long PLAYER_BURST_SPACING_MS = 150L;

    private final WeaponType type;
    private final String projectileDirectory;
    private final double baseScale;

    protected Weapon(WeaponType type, String projectileDirectory, double baseScale) {
        this.type = type;
        this.projectileDirectory = projectileDirectory;
        this.baseScale = baseScale;
    }

    public WeaponType getType() {
        return type;
    }

    public abstract void update(long deltaTimeMs, GameWorld world, PlayerSprite player, Player playerData);

    public void onUnequipped(GameWorld world) {
        // Default weapons do not need cleanup.
    }

    protected Enemy findNearestEnemyInRange(GameWorld world, int fromX, int fromY, int range) {
        Enemy nearestEnemy = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Enemy enemy : world.getEnemies()) {
            if (enemy.isDead()) {
                continue;
            }

            int deltaX = enemy.getCenterX() - fromX;
            int deltaY = enemy.getCenterY() - fromY;
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            if (distance <= range && distance < nearestDistance) {
                nearestDistance = distance;
                nearestEnemy = enemy;
            }
        }

        return nearestEnemy;
    }

    protected int getDamage(Player playerData) {
        return (int) Math.round(PLAYER_BASE_DAMAGE * playerData.getDamageMultiplier());
    }

    protected int getProjectileCount(Player playerData) {
        return Math.max(1, (int) Math.round(playerData.getProjectileCountMultiplier()));
    }

    protected double getProjectileScale(Player playerData) {
        return baseScale * playerData.getProjectileSizeMultiplier();
    }

    protected Projectile createProjectile(int startX, int startY, int targetX, int targetY,
                                          double speed, int damage, Player playerData,
                                          WeaponType weaponType, Projectile.MotionMode motionMode) {
        double deltaX = targetX - startX;
        double deltaY = targetY - startY;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (distance == 0) {
            distance = 1;
        }

        double velocityX = (deltaX / distance) * speed;
        double velocityY = (deltaY / distance) * speed;
        double hitboxLengthScale = 0.42;
        double hitboxThicknessScale = 0.16;
        double rotationRadians = Math.atan2(deltaY, deltaX);
        return new Projectile(startX, startY, velocityX, velocityY, damage, false,
            projectileDirectory, getProjectileScale(playerData), true, rotationRadians,
            hitboxLengthScale, hitboxThicknessScale, weaponType, motionMode);
    }
}

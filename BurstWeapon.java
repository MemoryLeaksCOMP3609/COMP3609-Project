public abstract class BurstWeapon extends Weapon {
    private final double projectileSpeed;
    private final long baseFireIntervalMs;
    private final Projectile.MotionMode motionMode;

    private long shotCooldownMs;
    private long burstCooldownMs;
    private int queuedBurstShots;

    protected BurstWeapon(WeaponType type, String projectileDirectory, double baseScale,
                          double projectileSpeed, long baseFireIntervalMs,
                          Projectile.MotionMode motionMode) {
        super(type, projectileDirectory, baseScale);
        this.projectileSpeed = projectileSpeed;
        this.baseFireIntervalMs = baseFireIntervalMs;
        this.motionMode = motionMode;
        this.shotCooldownMs = 0L;
        this.burstCooldownMs = 0L;
        this.queuedBurstShots = 0;
    }

    @Override
    public void update(long deltaTimeMs, GameWorld world, PlayerSprite player, Player playerData) {
        shotCooldownMs = Math.max(0L, shotCooldownMs - deltaTimeMs);
        burstCooldownMs = Math.max(0L, burstCooldownMs - deltaTimeMs);

        Enemy nearestEnemy = findNearestEnemyInRange(world, player.getCenterX(), player.getCenterY(), PLAYER_ATTACK_RANGE);
        if (nearestEnemy == null) {
            return;
        }

        if (shotCooldownMs <= 0 && queuedBurstShots <= 0) {
            queuedBurstShots = getProjectileCount(playerData);
            burstCooldownMs = 0L;
            shotCooldownMs = getFireInterval(playerData);
        }

        if (queuedBurstShots > 0 && burstCooldownMs <= 0) {
            fireProjectile(world, player, playerData, nearestEnemy);
            queuedBurstShots--;
            if (queuedBurstShots > 0) {
                burstCooldownMs = PLAYER_BURST_SPACING_MS;
            }
        }
    }

    protected int getProjectileCount(Player playerData) {
        return Math.min(5, super.getProjectileCount(playerData));
    }

    protected long getFireInterval(Player playerData) {
        return Math.max(80L, (long) Math.round(baseFireIntervalMs / playerData.getFireRateMultiplier()));
    }

    protected void fireProjectile(GameWorld world, PlayerSprite player, Player playerData, Enemy targetEnemy) {
        Projectile projectile = createProjectile(
            player.getCenterX(),
            player.getCenterY(),
            targetEnemy.getCenterX(),
            targetEnemy.getCenterY(),
            projectileSpeed,
            getDamage(playerData),
            playerData,
            getType(),
            motionMode
        );
        configureProjectile(projectile);
        world.getProjectiles().add(projectile);
    }

    protected void configureProjectile(Projectile projectile) {
        // Default burst weapons do not need extra setup.
    }
}

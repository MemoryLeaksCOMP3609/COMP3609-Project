import java.util.Iterator;

public class FireBallWeapon extends Weapon {
    private static final int FIRE_BALL_BASE_PROJECTILE_COUNT = 2;
    private static final double FIRE_BALL_ORBIT_RADIUS = 90.0;
    private static final double FIRE_BALL_BASE_ANGULAR_SPEED = 0.0035;
    private static final double FIRE_BALL_BASE_SCALE = 0.10;

    private int lastAppliedProjectileCount;
    private int lastAppliedDamage;
    private double lastAppliedProjectileScale;
    private double lastAppliedAngularSpeed;

    public FireBallWeapon() {
        super(WeaponType.FIRE_BALL, "images/spells/fireBall", FIRE_BALL_BASE_SCALE);
        resetCachedStats();
    }

    @Override
    public void update(long deltaTimeMs, GameWorld world, PlayerSprite player, Player playerData) {
        int projectileCount = getProjectileCount(playerData);
        double projectileScale = getProjectileScale(playerData);
        int damage = getDamage(playerData);
        double orbitAngularSpeed = FIRE_BALL_BASE_ANGULAR_SPEED * playerData.getFireRateMultiplier();
        int existingCount = 0;

        for (Projectile projectile : world.getProjectiles()) {
            if (projectile.getWeaponType() == WeaponType.FIRE_BALL && !projectile.isEnemyOwned()) {
                existingCount++;
            }
        }

        if (existingCount == projectileCount
            && projectileCount == lastAppliedProjectileCount
            && damage == lastAppliedDamage
            && Double.compare(projectileScale, lastAppliedProjectileScale) == 0
            && Double.compare(orbitAngularSpeed, lastAppliedAngularSpeed) == 0) {
            return;
        }

        removeOrbitingFireballs(world);

        for (int i = 0; i < projectileCount; i++) {
            double orbitAngle = (Math.PI * 2.0 * i) / projectileCount;
            Projectile fireball = createOrbitProjectile(player, damage, projectileScale, orbitAngle);
            fireball.configureOrbit(
                FIRE_BALL_ORBIT_RADIUS,
                orbitAngle,
                orbitAngularSpeed
            );
            world.getProjectiles().add(fireball);
        }

        lastAppliedProjectileCount = projectileCount;
        lastAppliedDamage = damage;
        lastAppliedProjectileScale = projectileScale;
        lastAppliedAngularSpeed = orbitAngularSpeed;
    }

    @Override
    public void onUnequipped(GameWorld world) {
        removeOrbitingFireballs(world);
        resetCachedStats();
    }

    private Projectile createOrbitProjectile(PlayerSprite player, int damage, double projectileScale, double orbitAngle) {
        return Projectile.create(
            player.getCenterX(),
            player.getCenterY(),
            0,
            0,
            damage,
            false,
            "images/spells/fireBall",
            projectileScale,
            true,
            orbitAngle,
            0.42,
            0.16,
            WeaponType.FIRE_BALL,
            Projectile.MotionMode.ORBIT
        );
    }

    @Override
    protected int getProjectileCount(Player playerData) {
        int bonusProjectiles = Math.max(0, (int) Math.round(playerData.getProjectileCountMultiplier()) - 1);
        return FIRE_BALL_BASE_PROJECTILE_COUNT + bonusProjectiles;
    }

    @Override
    public void appendHudStats(java.util.List<String> lines, Player playerData) {
        super.appendHudStats(lines, playerData);
        lines.add("Orbit Rate: " + formatMultiplier(playerData.getFireRateMultiplier()));
        lines.add("Orbit Count: " + getProjectileCount(playerData));
        lines.add("Projectile Size: " + formatMultiplier(playerData.getProjectileSizeMultiplier()));
    }

    private void removeOrbitingFireballs(GameWorld world) {
        Iterator<Projectile> iterator = world.getProjectiles().iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            if (projectile.getWeaponType() == WeaponType.FIRE_BALL && !projectile.isEnemyOwned()) {
                iterator.remove();
            }
        }
    }

    private void resetCachedStats() {
        lastAppliedProjectileCount = -1;
        lastAppliedDamage = -1;
        lastAppliedProjectileScale = -1.0;
        lastAppliedAngularSpeed = -1.0;
    }
}

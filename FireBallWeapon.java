import java.util.Iterator;

public class FireBallWeapon extends Weapon {
    private static final double FIRE_BALL_ORBIT_RADIUS = 90.0;
    private static final double FIRE_BALL_BASE_ANGULAR_SPEED = 0.0035;
    private static final double FIRE_BALL_BASE_SCALE = 0.10;

    public FireBallWeapon() {
        super(WeaponType.FIRE_BALL, "images/spells/fireBall", FIRE_BALL_BASE_SCALE);
    }

    @Override
    public void update(long deltaTimeMs, GameWorld world, PlayerSprite player, Player playerData) {
        int projectileCount = getProjectileCount(playerData);
        double projectileScale = getProjectileScale(playerData);
        int existingCount = 0;

        for (Projectile projectile : world.getProjectiles()) {
            if (projectile.getWeaponType() == WeaponType.FIRE_BALL && !projectile.isEnemyOwned()) {
                existingCount++;
            }
        }

        if (existingCount == projectileCount) {
            return;
        }

        removeOrbitingFireballs(world);

        int damage = getDamage(playerData);
        double fireRateMultiplier = playerData.getFireRateMultiplier();
        for (int i = 0; i < projectileCount; i++) {
            double orbitAngle = (Math.PI * 2.0 * i) / projectileCount;
            Projectile fireball = createOrbitProjectile(player, damage, projectileScale, orbitAngle);
            fireball.configureOrbit(
                FIRE_BALL_ORBIT_RADIUS,
                orbitAngle,
                FIRE_BALL_BASE_ANGULAR_SPEED * fireRateMultiplier
            );
            world.getProjectiles().add(fireball);
        }
    }

    @Override
    public void onUnequipped(GameWorld world) {
        removeOrbitingFireballs(world);
    }

    private Projectile createOrbitProjectile(PlayerSprite player, int damage, double projectileScale, double orbitAngle) {
        return new Projectile(
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

    private void removeOrbitingFireballs(GameWorld world) {
        Iterator<Projectile> iterator = world.getProjectiles().iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            if (projectile.getWeaponType() == WeaponType.FIRE_BALL && !projectile.isEnemyOwned()) {
                iterator.remove();
            }
        }
    }
}

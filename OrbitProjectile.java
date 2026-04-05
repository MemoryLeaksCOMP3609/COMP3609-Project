public class OrbitProjectile extends Projectile {
    private static final long ORBIT_IMPACT_COOLDOWN_MS = 250L;

    private double orbitRadius;
    private double orbitAngle;
    private double orbitAngularSpeed;

    public OrbitProjectile(double worldX, double worldY, double velocityX, double velocityY,
                           int damage, boolean enemyOwned, String frameDirectory,
                           double renderScale, boolean baseImageFacesLeft, double rotationRadians,
                           double hitboxLengthScale, double hitboxThicknessScale,
                           WeaponType weaponType) {
        super(worldX, worldY, velocityX, velocityY, damage, enemyOwned, frameDirectory, renderScale,
            baseImageFacesLeft, rotationRadians, hitboxLengthScale, hitboxThicknessScale,
            weaponType, MotionMode.ORBIT, Long.MAX_VALUE);
        this.orbitRadius = 120.0;
        this.orbitAngle = rotationRadians;
        this.orbitAngularSpeed = 0.0;
    }

    @Override
    protected void updateMotion(long deltaTimeMs, GameWorld world, PlayerSprite player) {
        if (player == null) {
            return;
        }

        orbitAngle += orbitAngularSpeed * deltaTimeMs;
        setWorldX(player.getCenterX() + Math.cos(orbitAngle) * orbitRadius);
        setWorldY(player.getCenterY() + Math.sin(orbitAngle) * orbitRadius);
        setRotationRadians(orbitAngle + Math.PI / 2.0);
    }

    @Override
    protected boolean ignoresWorldBounds() {
        return true;
    }

    @Override
    protected boolean usesContactCooldownOnImpact() {
        return true;
    }

    @Override
    protected long getImpactCooldownMs() {
        return ORBIT_IMPACT_COOLDOWN_MS;
    }

    @Override
    public void configureOrbit(double orbitRadius, double orbitAngle, double orbitAngularSpeed) {
        this.orbitRadius = orbitRadius;
        this.orbitAngle = orbitAngle;
        this.orbitAngularSpeed = orbitAngularSpeed;
    }
}

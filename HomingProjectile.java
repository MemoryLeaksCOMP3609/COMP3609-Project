public class HomingProjectile extends Projectile {
    private static final double MOVEMENT_REFERENCE_FRAME_MS = 40.0;

    private double homingTurnRateRadians;

    public HomingProjectile(double worldX, double worldY, double velocityX, double velocityY,
                            int damage, boolean enemyOwned, String frameDirectory,
                            double renderScale, boolean baseImageFacesLeft, double rotationRadians,
                            double hitboxLengthScale, double hitboxThicknessScale,
                            WeaponType weaponType) {
        super(worldX, worldY, velocityX, velocityY, damage, enemyOwned, frameDirectory, renderScale,
            baseImageFacesLeft, rotationRadians, hitboxLengthScale, hitboxThicknessScale,
            weaponType, MotionMode.HOMING, getDefaultMaxLifetimeMs());
        this.homingTurnRateRadians = 0.10;
    }

    @Override
    protected void updateMotion(long deltaTimeMs, GameWorld world, PlayerSprite player) {
        Enemy nearestEnemy = findNearestEnemy(world);
        if (nearestEnemy != null) {
            double targetAngle = Math.atan2(nearestEnemy.getCenterY() - getWorldY(), nearestEnemy.getCenterX() - getWorldX());
            double currentAngle = Math.atan2(getVelocityY(), getVelocityX());
            double deltaAngle = normalizeAngle(targetAngle - currentAngle);
            double angleStep = homingTurnRateRadians * (deltaTimeMs / MOVEMENT_REFERENCE_FRAME_MS);
            double appliedTurn = Math.max(-angleStep, Math.min(angleStep, deltaAngle));
            currentAngle += appliedTurn;

            double speed = Math.sqrt(getVelocityX() * getVelocityX() + getVelocityY() * getVelocityY());
            setVelocityX(Math.cos(currentAngle) * speed);
            setVelocityY(Math.sin(currentAngle) * speed);
            setRotationRadians(currentAngle);
        }

        moveStraight(deltaTimeMs, MOVEMENT_REFERENCE_FRAME_MS);
    }

    @Override
    public void configureHomingTurnRate(double homingTurnRateRadians) {
        this.homingTurnRateRadians = homingTurnRateRadians;
    }
}

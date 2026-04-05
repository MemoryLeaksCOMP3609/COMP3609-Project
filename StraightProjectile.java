public class StraightProjectile extends Projectile {
    private static final double MOVEMENT_REFERENCE_FRAME_MS = 40.0;

    public StraightProjectile(double worldX, double worldY, double velocityX, double velocityY,
                              int damage, boolean enemyOwned, String frameDirectory,
                              double renderScale, boolean baseImageFacesLeft, double rotationRadians,
                              double hitboxLengthScale, double hitboxThicknessScale,
                              WeaponType weaponType) {
        super(worldX, worldY, velocityX, velocityY, damage, enemyOwned, frameDirectory, renderScale,
            baseImageFacesLeft, rotationRadians, hitboxLengthScale, hitboxThicknessScale,
            weaponType, MotionMode.STRAIGHT, getDefaultMaxLifetimeMs());
    }

    @Override
    protected void updateMotion(long deltaTimeMs, GameWorld world, PlayerSprite player) {
        moveStraight(deltaTimeMs, MOVEMENT_REFERENCE_FRAME_MS);
    }
}

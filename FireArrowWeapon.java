public class FireArrowWeapon extends BurstWeapon {
    private static final double FIRE_ARROW_SPEED = 12.0;
    private static final long FIRE_ARROW_BASE_FIRE_INTERVAL_MS = 1500L;

    public FireArrowWeapon() {
        super(WeaponType.FIRE_ARROW, "images/spells/fireArrow", 0.20,
            FIRE_ARROW_SPEED, FIRE_ARROW_BASE_FIRE_INTERVAL_MS, Projectile.MotionMode.STRAIGHT);
    }
}

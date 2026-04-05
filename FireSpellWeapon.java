public class FireSpellWeapon extends BurstWeapon {
    private static final double FIRE_SPELL_SPEED = 9.0;
    private static final long FIRE_SPELL_BASE_FIRE_INTERVAL_MS = 2000L;

    public FireSpellWeapon() {
        super(WeaponType.FIRE_SPELL, "images/spells/fireSpell", 0.20,
            FIRE_SPELL_SPEED, FIRE_SPELL_BASE_FIRE_INTERVAL_MS, Projectile.MotionMode.HOMING);
    }

    @Override
    protected void configureProjectile(Projectile projectile) {
        projectile.configureHomingTurnRate(0.22);
    }
}

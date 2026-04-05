public final class WeaponFactory {
    private WeaponFactory() {
    }

    public static Weapon create(WeaponType weaponType) {
        if (weaponType == null) {
            return new FireArrowWeapon();
        }

        switch (weaponType) {
            case FIRE_BALL:
                return new FireBallWeapon();
            case FIRE_SPELL:
                return new FireSpellWeapon();
            case FIRE_ARROW:
            default:
                return new FireArrowWeapon();
        }
    }
}

public enum WeaponType {
    FIRE_ARROW("Fire Arrow"),
    FIRE_BALL("Fire Ball"),
    FIRE_SPELL("Fire Spell");

    private final String displayName;

    WeaponType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

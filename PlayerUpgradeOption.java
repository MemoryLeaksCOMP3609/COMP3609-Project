public enum PlayerUpgradeOption {
    MOVE_SPEED("Move Speed +1") {
        @Override
        public void apply(Player player) {
            player.increaseMoveSpeed(1);
        }
    },
    DAMAGE("Damage +0.15x") {
        @Override
        public void apply(Player player) {
            player.increaseDamageMultiplier(0.15);
        }
    },
    FIRE_RATE("Fire Rate +0.15x") {
        @Override
        public void apply(Player player) {
            player.increaseFireRateMultiplier(0.15);
        }
    },
    MAX_HEALTH("Max Health +20") {
        @Override
        public void apply(Player player) {
            player.increaseMaxHealth(20);
        }
    },
    HEALTH_REGEN("Health Regen +1 / 5s") {
        @Override
        public void apply(Player player) {
            player.increaseHealthRegenPerInterval(1);
        }
    },
    PROJECTILE_COUNT("Projectile Count +1") {
        @Override
        public void apply(Player player) {
            player.increaseProjectileCountMultiplier(1.0);
        }

        @Override
        public boolean isAvailable(Player player) {
            if (player == null || player.getLevel() % 2 != 0) {
                return false;
            }

            WeaponType weaponType = player.getWeaponType();
            if (weaponType == WeaponType.FIRE_ARROW || weaponType == WeaponType.FIRE_SPELL) {
                return player.getProjectileCountMultiplier() < 5.0;
            }

            return true;
        }
    },
    PROJECTILE_SIZE("Projectile Size +0.15x") {
        @Override
        public void apply(Player player) {
            player.increaseProjectileSizeMultiplier(0.15);
        }

        @Override
        public boolean isAvailable(Player player) {
            return player != null && player.getWeaponType() == WeaponType.FIRE_BALL;
        }
    };

    private final String displayName;

    PlayerUpgradeOption(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAvailable(Player player) {
        return true;
    }

    public abstract void apply(Player player);
}

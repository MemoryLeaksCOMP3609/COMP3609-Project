import java.awt.image.BufferedImage;

public enum PlayerUpgradeOption {
    MOVE_SPEED("Move Speed +1", "images/player/icons/moveSpeed.png") {
        @Override
        public void apply(Player player) {
            player.increaseMoveSpeed(1);
        }
    },
    DAMAGE("Damage +20%", "images/player/icons/damage.png") {
        @Override
        public void apply(Player player) {
            player.increaseDamageMultiplier(0.20);
        }
    },
    FIRE_RATE("Fire Rate +20%", "images/player/icons/fireRate.png") {
        @Override
        public void apply(Player player) {
            player.increaseFireRateMultiplier(0.20);
        }

        @Override
        public String getDisplayName(Player player) {
            if (player != null && player.getWeaponType() == WeaponType.FIRE_BALL) {
                return "Orbit Rate +20%";
            }

            return super.getDisplayName(player);
        }
    },
    MAX_HEALTH("Max Health +50", "images/player/icons/maxHealth.png") {
        @Override
        public void apply(Player player) {
            player.increaseMaxHealth(50);
        }
    },
    HEALTH_REGEN("Health Regen +10 / 5s", "images/player/icons/healthRegen.png") {
        @Override
        public void apply(Player player) {
            player.increaseHealthRegenPerInterval(10);
        }
    },
    PROJECTILE_COUNT("Projectile Count +1", "images/player/icons/count.png") {
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
    PROJECTILE_SIZE("Projectile Size +25%", "images/player/icons/size.png") {
        @Override
        public void apply(Player player) {
            player.increaseProjectileSizeMultiplier(0.25);
        }

        @Override
        public boolean isAvailable(Player player) {
            return player != null
                && player.getWeaponType() == WeaponType.FIRE_BALL
                && player.getProjectileSizeMultiplier() < Player.MAX_PROJECTILE_SIZE_MULTIPLIER;
        }
    };

    private final String displayName;
    private final String iconPath;
    private BufferedImage iconImage;

    PlayerUpgradeOption(String displayName, String iconPath) {
        this.displayName = displayName;
        this.iconPath = iconPath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayName(Player player) {
        return getDisplayName();
    }

    public boolean isAvailable(Player player) {
        return true;
    }

    public BufferedImage getIconImage() {
        if (iconImage == null) {
            iconImage = ImageManager.loadBufferedImage(iconPath);
        }
        return iconImage;
    }

    public abstract void apply(Player player);
}

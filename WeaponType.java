import java.awt.image.BufferedImage;

public enum WeaponType {
    FIRE_ARROW(
        "Fire Arrow",
        "images/spells/icons/Icons_Fire Arrow.png",
        "Fast, direct shots that reward good aim.",
        "Good against bats."
    ),
    FIRE_BALL(
        "Fire Ball",
        "images/spells/icons/Icons_Fire Ball.png",
        "A steady orbiting flame that burns nearby foes.",
        "Good against skeletons and bosses, but difficult to use."
    ),
    FIRE_SPELL(
        "Fire Spell",
        "images/spells/icons/Icons_Fire Spell.png",
        "Homing magic that chases targets automatically.",
        "Good against ghosts."
    );

    private final String displayName;
    private final String iconPath;
    private final String description;
    private final String detailText;
    private BufferedImage iconImage;

    WeaponType(String displayName, String iconPath, String description, String detailText) {
        this.displayName = displayName;
        this.iconPath = iconPath;
        this.description = description;
        this.detailText = detailText;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getDetailText() {
        return detailText;
    }

    public BufferedImage getIconImage() {
        if (iconImage == null) {
            iconImage = ImageManager.loadBufferedImage(iconPath);
        }
        return iconImage;
    }
}

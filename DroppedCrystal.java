import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class DroppedCrystal {
    public enum CrystalType {
        EXPERIENCE,
        HEALTH
    }

    public enum ExperienceTier {
        TIER_1(5),
        TIER_2(10),
        TIER_3(15),
        TIER_4(20);

        private final int experienceValue;

        ExperienceTier(int experienceValue) {
            this.experienceValue = experienceValue;
        }

        public int getExperienceValue() {
            return experienceValue;
        }
    }

    private final int worldX;
    private final int worldY;
    private final int width;
    private final int height;
    private final CrystalType type;
    private final ExperienceTier experienceTier;
    private final BufferedImage image;
    private int screenX;
    private int screenY;
    private boolean collected;

    public DroppedCrystal(int worldX, int worldY, BufferedImage image, CrystalType type, ExperienceTier experienceTier) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.image = image;
        this.type = type;
        this.experienceTier = experienceTier;
        this.width = image != null ? image.getWidth() : 1;
        this.height = image != null ? image.getHeight() : 1;
        this.screenX = worldX;
        this.screenY = worldY;
        this.collected = false;
    }

    public void updateScreenPosition(int cameraX, int cameraY) {
        screenX = worldX - cameraX;
        screenY = worldY - cameraY;
    }

    public void draw(Graphics2D g2) {
        if (collected || image == null) {
            return;
        }
        g2.drawImage(image, screenX, screenY, null);
    }

    public Rectangle2D.Double getBoundingRectangle() {
        return new Rectangle2D.Double(worldX, worldY, width, height);
    }

    public CrystalType getType() {
        return type;
    }

    public int getExperienceValue() {
        return experienceTier != null ? experienceTier.getExperienceValue() : 0;
    }

    public boolean isCollected() {
        return collected;
    }

    public void collect() {
        collected = true;
    }
}

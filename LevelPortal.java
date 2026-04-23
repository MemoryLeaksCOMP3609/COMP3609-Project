import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class LevelPortal {

    private BufferedImage spriteSheet;
    private int frameWidth;
    private int frameHeight;

    private int currentFrame;
    private int totalFrames;
    private int animTimer;
    private static final int ANIM_SPEED = 8;

    private final int worldX;
    private final int worldY;

    private int portalLevel = 1;

    // True once the region boss has been defeated.
    private boolean regionBossDead = false;

    public LevelPortal(int worldX, int worldY) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.currentFrame = 0;
        this.totalFrames = 9; // 3×3 sprite sheet
        this.animTimer = 0;

        try {
            spriteSheet = ImageIO.read(new File("images/objects/portal.png"));
            frameWidth = spriteSheet.getWidth() / 3;
            frameHeight = spriteSheet.getHeight() / 3;
            System.out.println("Portal loaded. Frame size: " + frameWidth + "x" + frameHeight);
        } catch (IOException e) {
            System.out.println("Failed to load portal: " + e.getMessage());
            // Provide a safe default so getBoundingRectangle() never returns zero-size
            frameWidth = 100;
            frameHeight = 100;
        }
    }

    public void setPortalLevel(int level) {
        this.portalLevel = level;
    }

    public int getPortalLevel() {
        return portalLevel;
    }

    /**
     * Returns true only when both conditions are satisfied:
     * • the region boss is dead
     * • the player has hit the minimum level for advancing
     *
     * @param playerLevel The current level of the player (Player.getLevel()).
     */
    public boolean isBossDead(int playerLevel) {
        if (!regionBossDead)
            return false;

        int requiredLevel;
        switch (portalLevel) {
            case 1:
                requiredLevel = 10;
                break; // grass → desert
            case 2:
                requiredLevel = 15;
                break; // desert → ice
            case 3:
                requiredLevel = 20;
                break; // ice → victory
            default:
                requiredLevel = 10;
                break;
        }

        return playerLevel >= requiredLevel;
    }

    public boolean isBossDead() {
        return regionBossDead;
    }

    public void notifyBossDead() {
        this.regionBossDead = true;
        System.out.println("Portal " + portalLevel + ": region boss confirmed dead.");
    }

    public void setBossDead(boolean dead) {
        this.regionBossDead = dead;
    }

    public void update() {
        animTimer++;
        if (animTimer >= ANIM_SPEED) {
            animTimer = 0;
            currentFrame = (currentFrame + 1) % totalFrames;
        }
    }

    public void draw(Graphics2D g2, int cameraX, int cameraY) {
        if (spriteSheet == null)
            return;

        int col = currentFrame % 3;
        int row = currentFrame / 3;

        BufferedImage frame = spriteSheet.getSubimage(
                col * frameWidth,
                row * frameHeight,
                frameWidth,
                frameHeight);

        int screenX = worldX - cameraX;
        int screenY = worldY - cameraY;

        g2.drawImage(frame, screenX, screenY, frameWidth, frameHeight, null);
    }

    public boolean collidesWithPlayer(Rectangle2D.Double playerBounds) {
        return getBoundingRectangle().intersects(playerBounds);
    }

    public Rectangle2D.Double getBoundingRectangle() {
        return new Rectangle2D.Double(worldX, worldY, frameWidth, frameHeight);
    }

    public int getWorldX() {
        return worldX;
    }

    public int getWorldY() {
        return worldY;
    }
}

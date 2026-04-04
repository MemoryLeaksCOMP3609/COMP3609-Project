import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

public class Projectile {
    private static final double MOVEMENT_REFERENCE_FRAME_MS = 40.0;

    private double worldX;
    private double worldY;
    private final double velocityX;
    private final double velocityY;
    private final int radius;
    private final int damage;
    private final boolean enemyOwned;
    private final Color color;
    private boolean active;

    public Projectile(double worldX, double worldY, double velocityX, double velocityY,
                      int radius, int damage, boolean enemyOwned, Color color) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.radius = radius;
        this.damage = damage;
        this.enemyOwned = enemyOwned;
        this.color = color;
        this.active = true;
    }

    public void update(long deltaTimeMs) {
        double distanceScale = deltaTimeMs / MOVEMENT_REFERENCE_FRAME_MS;
        worldX += velocityX * distanceScale;
        worldY += velocityY * distanceScale;
    }

    public void draw(Graphics2D g2, int cameraX, int cameraY) {
        if (!active) {
            return;
        }

        int drawX = (int) Math.round(worldX) - cameraX;
        int drawY = (int) Math.round(worldY) - cameraY;

        g2.setColor(color);
        g2.fillOval(drawX - radius, drawY - radius, radius * 2, radius * 2);
    }

    public Ellipse2D.Double getBounds() {
        return new Ellipse2D.Double(worldX - radius, worldY - radius, radius * 2.0, radius * 2.0);
    }

    public boolean isOutOfBounds(int worldWidth, int worldHeight) {
        return worldX < -radius || worldY < -radius || worldX > worldWidth + radius || worldY > worldHeight + radius;
    }

    public int getDamage() {
        return damage;
    }

    public boolean isEnemyOwned() {
        return enemyOwned;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }
}

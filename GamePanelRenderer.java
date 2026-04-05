import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class GamePanelRenderer {
    private static final int PLAYER_HEALTH_BAR_WIDTH = 150;
    private static final int ENEMY_HEALTH_BAR_WIDTH = 100;
    private static final int HEALTH_BAR_HEIGHT = 10;
    private static final int HEALTH_BAR_VERTICAL_OFFSET = 12;
    private static final int HEALTH_BAR_BORDER_THICKNESS = 2;

    private final int worldWidth;
    private final int worldHeight;
    private final int goldenTintColor;

    public GamePanelRenderer(int worldWidth, int worldHeight, int goldenTintColor) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.goldenTintColor = goldenTintColor;
    }

    public void draw(Graphics2D g2, int panelWidth, int panelHeight, GameWorld world,
                     GameSessionState sessionState, ArrayList<ImageFX> effects,
                     GrayScaleFX screenGrayScaleFX, BufferedImage doubleBufferImage) {
        boolean applyGrayScale = sessionState.isGameOver() && screenGrayScaleFX != null;

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, panelWidth, panelHeight);

        if (world.getBackgroundImage() != null) {
            g2.drawImage(world.getBackgroundImage(), -world.getCameraX(), -world.getCameraY(), worldWidth, worldHeight, null);
        }

        if (!sessionState.isGameRunning() && !sessionState.isGameOver()) {
            drawStartScreen(g2, panelWidth, panelHeight);
            return;
        }

        drawWorld(g2, panelWidth, panelHeight, world);

        for (ImageFX effect : effects) {
            effect.draw(g2);
        }

        if (sessionState.isGoldenTintActive() && !applyGrayScale) {
            g2.setColor(new Color(
                (goldenTintColor >> 16) & 0xFF,
                (goldenTintColor >> 8) & 0xFF,
                goldenTintColor & 0xFF,
                (goldenTintColor >> 24) & 0xFF
            ));
            g2.fillRect(0, 0, panelWidth, panelHeight);
        }

        if (applyGrayScale && doubleBufferImage != null) {
            drawGrayScaleOverlay(g2, panelWidth, panelHeight, doubleBufferImage);
        }

        if (sessionState.isGameOver()) {
            drawGameOver(g2, panelWidth, panelHeight, applyGrayScale);
        }
    }

    private void drawStartScreen(Graphics2D g2, int panelWidth, int panelHeight) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 36));
        String title = "Coin Collector";
        int titleX = (panelWidth - g2.getFontMetrics().stringWidth(title)) / 2;
        int titleY = panelHeight / 2 - 20;
        g2.drawString(title, titleX, titleY);
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        String subtitle = "Press Start to begin";
        int subtitleX = (panelWidth - g2.getFontMetrics().stringWidth(subtitle)) / 2;
        int subtitleY = titleY + 50;
        g2.drawString(subtitle, subtitleX, subtitleY);
    }

    private void drawWorld(Graphics2D g2, int panelWidth, int panelHeight, GameWorld world) {
        g2.setColor(new Color(100, 100, 100));
        for (SolidObject solid : world.getSolidObjects()) {
            if (isVisibleOnScreen(solid.getBoundingRectangle(), panelWidth, panelHeight, world)) {
                solid.draw(g2, world.getCameraX(), world.getCameraY());
            }
        }

        for (Collectible collectible : world.getCollectibles()) {
            collectible.draw(g2);
        }

        for (Enemy enemy : world.getEnemies()) {
            if (isVisibleOnScreen(enemy.getBoundingRectangle(), panelWidth, panelHeight, world)) {
                enemy.draw(g2);
            }
        }

        for (DroppedCrystal crystal : world.getDroppedCrystals()) {
            if (isVisibleOnScreen(crystal.getBoundingRectangle(), panelWidth, panelHeight, world)) {
                crystal.draw(g2);
            }
        }

        for (AnimatedSprite sprite : world.getAnimatedSprites()) {
            sprite.draw(g2);
        }

        if (world.getPlayer() != null) {
            world.getPlayer().draw(g2);
        }

        if (world.getArrowSprite() != null) {
            world.getArrowSprite().draw(g2);
        }

        for (Projectile projectile : world.getProjectiles()) {
            if (isVisibleOnScreen(projectile.getBounds(), panelWidth, panelHeight, world)) {
                projectile.draw(g2, world.getCameraX(), world.getCameraY());
            }
        }

        drawHealthBars(g2, panelWidth, panelHeight, world);
    }

    private void drawGrayScaleOverlay(Graphics2D g2, int panelWidth, int panelHeight, BufferedImage doubleBufferImage) {
        BufferedImage grayImage = new BufferedImage(panelWidth, panelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gGray = grayImage.createGraphics();
        gGray.drawImage(doubleBufferImage, 0, 0, null);
        gGray.dispose();

        int[] pixels = new int[panelWidth * panelHeight];
        grayImage.getRGB(0, 0, panelWidth, panelHeight, pixels, 0, panelWidth);

        for (int i = 0; i < pixels.length; i++) {
            int alpha = (pixels[i] >> 24) & 255;
            int red = (pixels[i] >> 16) & 255;
            int green = (pixels[i] >> 8) & 255;
            int blue = pixels[i] & 255;
            int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
            pixels[i] = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
        }

        grayImage.setRGB(0, 0, panelWidth, panelHeight, pixels, 0, panelWidth);
        g2.drawImage(grayImage, 0, 0, null);
    }

    private void drawGameOver(Graphics2D g2, int panelWidth, int panelHeight, boolean applyGrayScale) {
        if (!applyGrayScale) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, panelWidth, panelHeight);
        }

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 48));
        String message = "Game Over";
        int textX = (panelWidth - g2.getFontMetrics().stringWidth(message)) / 2;
        int textY = panelHeight / 2;
        g2.drawString(message, textX, textY);
    }

    private boolean isVisibleOnScreen(Rectangle2D.Double worldBounds, int panelWidth, int panelHeight, GameWorld world) {
        if (worldBounds == null) {
            return false;
        }

        Rectangle2D.Double viewport = new Rectangle2D.Double(
            world.getCameraX(),
            world.getCameraY(),
            panelWidth,
            panelHeight
        );
        return viewport.intersects(worldBounds);
    }

    private void drawHealthBars(Graphics2D g2, int panelWidth, int panelHeight, GameWorld world) {
        PlayerSprite playerSprite = world.getPlayer();
        Player playerData = world.getPlayerData();
        if (playerSprite != null && playerData != null && playerData.getMaxHealth() > 0) {
            Rectangle2D.Double playerBounds = playerSprite.getBoundingRectangle();
            drawHealthBar(
                g2,
                playerBounds,
                world,
                panelWidth,
                panelHeight,
                PLAYER_HEALTH_BAR_WIDTH,
                playerData.getHealth(),
                playerData.getMaxHealth()
            );
        }

        for (Enemy enemy : world.getEnemies()) {
            if (enemy == null || enemy.getMaxHealth() <= 0 || enemy.getCurrentHealth() <= 0) {
                continue;
            }

            Rectangle2D.Double enemyBounds = enemy.getBoundingRectangle();
            if (!isVisibleOnScreen(enemyBounds, panelWidth, panelHeight, world)) {
                continue;
            }

            int healthBarWidth = enemy instanceof BossEnemy
                ? Math.max(1, (int) Math.round(enemyBounds.getWidth()))
                : ENEMY_HEALTH_BAR_WIDTH;
            drawHealthBar(
                g2,
                enemyBounds,
                world,
                panelWidth,
                panelHeight,
                healthBarWidth,
                enemy.getCurrentHealth(),
                enemy.getMaxHealth()
            );
        }
    }

    private void drawHealthBar(Graphics2D g2, Rectangle2D.Double worldBounds, GameWorld world,
                               int panelWidth, int panelHeight, int barWidth,
                               int currentHealth, int maxHealth) {
        if (worldBounds == null || maxHealth <= 0 || barWidth <= 0) {
            return;
        }

        int screenX = (int) Math.round(worldBounds.getX() - world.getCameraX());
        int screenY = (int) Math.round(worldBounds.getY() - world.getCameraY());
        int entityWidth = Math.max(1, (int) Math.round(worldBounds.getWidth()));
        int barX = screenX + (entityWidth - barWidth) / 2;
        int barY = screenY - HEALTH_BAR_VERTICAL_OFFSET - HEALTH_BAR_HEIGHT;

        if (barX + barWidth < 0 || barX > panelWidth || barY + HEALTH_BAR_HEIGHT < 0 || barY > panelHeight) {
            return;
        }

        float healthRatio = Math.max(0.0f, Math.min(1.0f, currentHealth / (float) maxHealth));
        int fillWidth = Math.round(barWidth * healthRatio);

        g2.setColor(new Color(35, 35, 35, 220));
        g2.fillRoundRect(barX, barY, barWidth, HEALTH_BAR_HEIGHT, 8, 8);

        if (fillWidth > 0) {
            g2.setColor(new Color(210, 60, 60));
            g2.fillRoundRect(barX, barY, fillWidth, HEALTH_BAR_HEIGHT, 8, 8);
        }

        g2.setColor(Color.WHITE);
        g2.drawRoundRect(
            barX - HEALTH_BAR_BORDER_THICKNESS / 2,
            barY - HEALTH_BAR_BORDER_THICKNESS / 2,
            barWidth + HEALTH_BAR_BORDER_THICKNESS - 1,
            HEALTH_BAR_HEIGHT + HEALTH_BAR_BORDER_THICKNESS - 1,
            8,
            8
        );
    }
}

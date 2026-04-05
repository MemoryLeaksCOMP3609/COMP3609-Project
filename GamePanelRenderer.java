import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class GamePanelRenderer {
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
            drawStartScreen(g2);
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

    private void drawStartScreen(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 36));
        g2.drawString("Coin Collector", 275, 280);
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.drawString("Press Start to begin", 320, 330);
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
        g2.drawString("Game Over", 270, 280);
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
}

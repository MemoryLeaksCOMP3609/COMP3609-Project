import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.awt.FontMetrics;

public class GamePanelRenderer {

    private static final int PLAYER_HEALTH_BAR_WIDTH = 200;
    private static final int ENEMY_HEALTH_BAR_WIDTH = 100;
    private static final int HEALTH_BAR_HEIGHT = 10;
    private static final int HEALTH_BAR_VERTICAL_OFFSET = 12;
    private static final int HEALTH_BAR_BORDER_THICKNESS = 2;
    private static final int EXPERIENCE_BAR_HEIGHT = 15;

    private final int worldWidth;
    private final int worldHeight;
    private final int goldenTintColor;
    private final GameStats gameStats;

    public GamePanelRenderer(int worldWidth, int worldHeight, int goldenTintColor, GameStats gameStats) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.goldenTintColor = goldenTintColor;
        this.gameStats = gameStats;
    }

    public void draw(Graphics2D g2, int panelWidth, int panelHeight,
            GameWorld world, GameSessionState sessionState,
            ArrayList<ImageFX> effects, GrayScaleFX screenGrayScaleFX,
            BufferedImage doubleBufferImage, int fadeAlpha) {

        boolean applyGrayScale = sessionState.isGameOver() && screenGrayScaleFX != null;

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, panelWidth, panelHeight);

        TileMap tileMap = world.getTileMap();
        if (tileMap != null) {
            tileMap.draw(g2, -world.getCameraX(), -world.getCameraY(), panelWidth, panelHeight);
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, panelWidth, panelHeight);
        }

        if (!sessionState.isGameRunning() && !sessionState.isGameOver()) {
            drawStartScreen(g2, panelWidth, panelHeight);
            return;
        }

        drawWorld(g2, panelWidth, panelHeight, world);

        for (ImageFX effect : effects)
            effect.draw(g2);

        if (sessionState.isGoldenTintActive() && !applyGrayScale) {
            g2.setColor(new Color(
                    (goldenTintColor >> 16) & 0xFF,
                    (goldenTintColor >> 8) & 0xFF,
                    goldenTintColor & 0xFF,
                    (goldenTintColor >> 24) & 0xFF));
            g2.fillRect(0, 0, panelWidth, panelHeight);
        }

        if (applyGrayScale && doubleBufferImage != null) {
            drawGrayScaleOverlay(g2, panelWidth, panelHeight, doubleBufferImage);
        }

        drawExperienceBar(g2, panelWidth, panelHeight, world.getPlayerData());

        if (world.isOverlayActive()) {
            if (world.isScoreboardActive()) {
                drawScoreboard(g2, panelWidth, panelHeight, gameStats, world.getOverlayAlpha() / 255f);
            } else {
                drawBossOverlay(g2, panelWidth, panelHeight, world);
            }
        }

        if (fadeAlpha > 0) {
            g2.setColor(new Color(0, 0, 0, fadeAlpha));
            g2.fillRect(0, 0, panelWidth, panelHeight);
        }

        if (sessionState.isGameOver()) {
            drawGameOver(g2, panelWidth, panelHeight, applyGrayScale);
        }
    }

    private void drawBossOverlay(Graphics2D g2, int panelWidth, int panelHeight,
            GameWorld world) {
        BufferedImage img = world.getOverlayImage();
        if (img == null)
            return;

        int alpha = world.getOverlayAlpha(); // 0-255
        if (alpha <= 0)
            return;

        int backdropAlpha = Math.min(200, alpha);
        g2.setColor(new Color(0, 0, 0, backdropAlpha));
        g2.fillRect(0, 0, panelWidth, panelHeight);

        int maxW = (int) (panelWidth * 0.60);
        int maxH = (int) (panelHeight * 0.60);
        double scaleW = (double) maxW / img.getWidth();
        double scaleH = (double) maxH / img.getHeight();
        double scale = Math.min(scaleW, scaleH);
        int drawW = (int) (img.getWidth() * scale);
        int drawH = (int) (img.getHeight() * scale);
        int drawX = (panelWidth - drawW) / 2;
        int drawY = (panelHeight - drawH) / 2;

        Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, drawX, drawY, drawW, drawH, null);
        g2.setComposite(originalComposite);
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
        g2.drawString(subtitle, subtitleX, titleY + 50);
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
            LevelPortal portal = world.getLevelPortal();
            if (portal != null) {
                portal.draw(g2, world.getCameraX(), world.getCameraY());
            }
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

    private void drawGrayScaleOverlay(Graphics2D g2, int panelWidth, int panelHeight,
            BufferedImage doubleBufferImage) {
        BufferedImage grayImage = new BufferedImage(panelWidth, panelHeight,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D gGray = grayImage.createGraphics();
        gGray.drawImage(doubleBufferImage, 0, 0, null);
        gGray.dispose();

        int[] pixels = new int[panelWidth * panelHeight];
        grayImage.getRGB(0, 0, panelWidth, panelHeight, pixels, 0, panelWidth);
        for (int i = 0; i < pixels.length; i++) {
            int a = (pixels[i] >> 24) & 255;
            int r = (pixels[i] >> 16) & 255;
            int g = (pixels[i] >> 8) & 255;
            int b = pixels[i] & 255;
            int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            pixels[i] = (a << 24) | (gray << 16) | (gray << 8) | gray;
        }
        grayImage.setRGB(0, 0, panelWidth, panelHeight, pixels, 0, panelWidth);
        g2.drawImage(grayImage, 0, 0, null);
    }

    private void drawGameOver(Graphics2D g2, int panelWidth, int panelHeight,
            boolean applyGrayScale) {
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

    private void drawHealthBars(Graphics2D g2, int panelWidth, int panelHeight,
            GameWorld world) {
        PlayerSprite playerSprite = world.getPlayer();
        Player playerData = world.getPlayerData();
        if (playerSprite != null && playerData != null && playerData.getMaxHealth() > 0) {
            drawHealthBar(g2, playerSprite.getBoundingRectangle(), world,
                    panelWidth, panelHeight,
                    PLAYER_HEALTH_BAR_WIDTH,
                    playerData.getHealth(), playerData.getMaxHealth());
        }

        for (Enemy enemy : world.getEnemies()) {
            if (enemy == null || enemy.getMaxHealth() <= 0 || enemy.getCurrentHealth() <= 0)
                continue;
            Rectangle2D.Double bounds = enemy.getBoundingRectangle();
            if (!isVisibleOnScreen(bounds, panelWidth, panelHeight, world))
                continue;

            int barWidth = enemy instanceof BossEnemy
                    ? Math.max(1, (int) Math.round(bounds.getWidth()))
                    : ENEMY_HEALTH_BAR_WIDTH;
            drawHealthBar(g2, bounds, world, panelWidth, panelHeight,
                    barWidth, enemy.getCurrentHealth(), enemy.getMaxHealth());
        }
    }

    private void drawHealthBar(Graphics2D g2, Rectangle2D.Double worldBounds,
            GameWorld world, int panelWidth, int panelHeight,
            int barWidth, int currentHealth, int maxHealth) {
        if (worldBounds == null || maxHealth <= 0 || barWidth <= 0)
            return;

        int screenX = (int) Math.round(worldBounds.getX() - world.getCameraX());
        int screenY = (int) Math.round(worldBounds.getY() - world.getCameraY());
        int entityWidth = Math.max(1, (int) Math.round(worldBounds.getWidth()));
        int barX = screenX + (entityWidth - barWidth) / 2;
        int barY = screenY - HEALTH_BAR_VERTICAL_OFFSET - HEALTH_BAR_HEIGHT;

        if (barX + barWidth < 0 || barX > panelWidth
                || barY + HEALTH_BAR_HEIGHT < 0 || barY > panelHeight)
            return;

        float healthRatio = Math.max(0f, Math.min(1f, currentHealth / (float) maxHealth));
        int fillWidth = Math.round(barWidth * healthRatio);

        g2.setColor(new Color(35, 35, 35, 220));
        g2.fillRoundRect(barX, barY, barWidth, HEALTH_BAR_HEIGHT, 8, 8);

        if (fillWidth > 0) {
            g2.setColor(new Color(210, 60, 60));
            g2.fillRoundRect(barX, barY, fillWidth, HEALTH_BAR_HEIGHT, 8, 8);
        }

        g2.setColor(Color.WHITE);
        g2.drawRoundRect(barX - HEALTH_BAR_BORDER_THICKNESS / 2,
                barY - HEALTH_BAR_BORDER_THICKNESS / 2,
                barWidth + HEALTH_BAR_BORDER_THICKNESS - 1,
                HEALTH_BAR_HEIGHT + HEALTH_BAR_BORDER_THICKNESS - 1,
                8, 8);
    }

    private void drawExperienceBar(Graphics2D g2, int panelWidth, int panelHeight,
            Player player) {
        if (player == null || panelWidth <= 0 || panelHeight <= 0)
            return;

        int barY = panelHeight - EXPERIENCE_BAR_HEIGHT;
        float ratio;
        if (player.isMaxLevel()) {
            ratio = 1f;
        } else if (player.getExperienceToNextLevel() > 0) {
            ratio = Math.max(0f, Math.min(1f,
                    player.getExperience() / (float) player.getExperienceToNextLevel()));
        } else {
            ratio = 0f;
        }

        int fillWidth = Math.round(panelWidth * ratio);

        g2.setColor(new Color(10, 18, 40, 220));
        g2.fillRect(0, barY, panelWidth, EXPERIENCE_BAR_HEIGHT);

        if (fillWidth > 0) {
            g2.setColor(new Color(40, 140, 255, 230));
            g2.fillRect(0, barY, fillWidth, EXPERIENCE_BAR_HEIGHT);
        }

        g2.setColor(new Color(185, 220, 255, 220));
        g2.drawLine(0, barY, panelWidth - 1, barY);
    }

    private boolean isVisibleOnScreen(Rectangle2D.Double worldBounds,
            int panelWidth, int panelHeight,
            GameWorld world) {
        if (worldBounds == null)
            return false;
        Rectangle2D.Double viewport = new Rectangle2D.Double(
                world.getCameraX(), world.getCameraY(), panelWidth, panelHeight);
        return viewport.intersects(worldBounds);
    }

    private void drawScoreboard(Graphics2D g2, int panelWidth, int panelHeight,
            GameStats stats, float overlayAlpha) {
        if (stats == null)
            return;

        // Draw semi-transparent black background
        int backdropAlpha = Math.min(200, (int) (overlayAlpha * 255));
        g2.setColor(new Color(0, 0, 0, backdropAlpha));
        g2.fillRect(0, 0, panelWidth, panelHeight);

        // Scoreboard dimensions and positioning
        int boardWidth = 650;
        int boardHeight = 580;
        int boardX = (panelWidth - boardWidth) / 2;
        int boardY = (panelHeight - boardHeight) / 2;

        // Draw scoreboard background and border
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(boardX, boardY, boardWidth, boardHeight);

        // Draw white border
        g2.setColor(new Color(255, 255, 255, (int) (overlayAlpha * 255)));
        g2.setStroke(new java.awt.BasicStroke(3));
        g2.drawRect(boardX, boardY, boardWidth, boardHeight);

        // Set up text rendering
        Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));

        g2.setColor(Color.WHITE);
        Font titleFont = new Font("Arial", Font.BOLD, 28);
        Font headerFont = new Font("Arial", Font.BOLD, 16);
        Font dataFont = new Font("Arial", Font.PLAIN, 14);
        Font indentedFont = new Font("Arial", Font.PLAIN, 12);

        int textX = boardX + 25;
        int textY = boardY + 35;
        int lineHeight = 22;

        g2.setFont(titleFont);
        g2.drawString("FINAL STATISTICS", textX, textY);
        textY += lineHeight + 5;

        g2.setStroke(new java.awt.BasicStroke(1));
        g2.drawLine(boardX + 15, textY - 5, boardX + boardWidth - 15, textY - 5);
        textY += 5;

        // HP Accumulated
        g2.setFont(headerFont);
        g2.drawString("HP Accumulated:", textX, textY);
        g2.setFont(dataFont);
        g2.drawString("+" + stats.getHPAccumulated() + " HP", textX + 350, textY);
        textY += lineHeight;

        // Crystals Collected
        g2.setFont(headerFont);
        g2.drawString("Crystals Collected:", textX, textY);
        g2.setFont(dataFont);
        g2.drawString(String.valueOf(stats.getTotalCrystalsCollected()), textX + 350, textY);
        textY += lineHeight - 2;

        // Crystal tiers
        g2.setFont(indentedFont);
        textY += 3;
        g2.drawString("├─ Tier 1: " + stats.getCrystalCountByTier(DroppedCrystal.ExperienceTier.TIER_1), textX + 20,
                textY);
        textY += lineHeight - 4;
        g2.drawString("├─ Tier 2: " + stats.getCrystalCountByTier(DroppedCrystal.ExperienceTier.TIER_2), textX + 20,
                textY);
        textY += lineHeight - 4;
        g2.drawString("├─ Tier 3: " + stats.getCrystalCountByTier(DroppedCrystal.ExperienceTier.TIER_3), textX + 20,
                textY);
        textY += lineHeight - 4;
        g2.drawString("└─ Tier 4: " + stats.getCrystalCountByTier(DroppedCrystal.ExperienceTier.TIER_4), textX + 20,
                textY);
        textY += lineHeight + 2;

        // Experience Gained
        g2.setFont(headerFont);
        g2.drawString("Total Experience Gained:", textX, textY);
        g2.setFont(dataFont);
        g2.drawString(stats.getTotalExperienceGained() + " XP", textX + 350, textY);
        textY += lineHeight;

        // Hearts Collected
        g2.setFont(headerFont);
        g2.drawString("Hearts Collected:", textX, textY);
        g2.setFont(dataFont);
        g2.drawString(String.valueOf(stats.getHeartsCollected()), textX + 350, textY);
        textY += lineHeight;

        // Speed Boosts Collected
        g2.setFont(headerFont);
        g2.drawString("Speed Boosts Activated:", textX, textY);
        g2.setFont(dataFont);
        g2.drawString(String.valueOf(stats.getSpeedBoostsActivated()), textX + 350, textY);
        textY += lineHeight;

        // Total Enemies Defeated
        g2.setFont(headerFont);
        g2.drawString("Total Enemies Defeated:", textX, textY);
        g2.setFont(dataFont);
        g2.drawString(String.valueOf(stats.getTotalEnemiesDefeated()), textX + 350, textY);
        textY += lineHeight - 2;

        // Enemy types
        g2.setFont(indentedFont);
        textY += 3;
        g2.drawString("├─ Bats: " + stats.getEnemyDefeatCount("Bat"), textX + 20, textY);
        textY += lineHeight - 4;
        g2.drawString("├─ Normal Skeletons: " + stats.getEnemyDefeatCount("NormalSkeleton"), textX + 20, textY);
        textY += lineHeight - 4;
        g2.drawString("├─ Normal Ghosts: " + stats.getEnemyDefeatCount("NormalGhost"), textX + 20, textY);
        textY += lineHeight - 4;
        g2.drawString("├─ Toxic Skeletons: " + stats.getEnemyDefeatCount("ToxicSkeleton"), textX + 20, textY);
        textY += lineHeight - 4;
        g2.drawString("├─ Dark Ghosts: " + stats.getEnemyDefeatCount("DarkGhost"), textX + 20, textY);
        textY += lineHeight - 4;
        g2.drawString("└─ Frost Ghosts: " + stats.getEnemyDefeatCount("FrostGhost"), textX + 20, textY);
        textY += lineHeight + 2;

        // Total Bosses Defeated
        g2.setFont(headerFont);
        g2.drawString("Bosses Defeated:", textX, textY);
        g2.setFont(dataFont);
        g2.drawString(String.valueOf(stats.getTotalBossesDefeated()), textX + 350, textY);
        textY += lineHeight - 2;

        // Boss types
        g2.setFont(indentedFont);
        textY += 3;
        g2.drawString("├─ Phase One: " + stats.getBossDefeatCount("BossPhaseOne"), textX + 20, textY);
        textY += lineHeight - 4;
        g2.drawString("├─ Phase Two: " + stats.getBossDefeatCount("BossPhaseTwo"), textX + 20, textY);
        textY += lineHeight - 4;
        g2.drawString("├─ Phase Three: " + stats.getBossDefeatCount("BossPhaseThree"), textX + 20, textY);
        textY += lineHeight - 4;
        g2.drawString("├─ Micro: " + stats.getBossDefeatCount("BossPhaseThreeMicro"), textX + 20, textY);
        textY += lineHeight - 4;
        g2.drawString("└─ Mini: " + stats.getBossDefeatCount("BossPhaseThreeMini"), textX + 20, textY);
        textY += lineHeight + 2;

        // Time Elapsed
        g2.setFont(headerFont);
        g2.drawString("Time Elapsed:", textX, textY);
        g2.setFont(dataFont);
        g2.drawString(stats.getFormattedTime(), textX + 350, textY);
        textY += lineHeight + 5;

        // Bottom border
        g2.setStroke(new java.awt.BasicStroke(1));
        g2.drawLine(boardX + 15, textY, boardX + boardWidth - 15, textY);
        textY += 12;

        g2.setFont(new Font("Arial", Font.BOLD, 14));
        String continueText = "Press any key to restart";
        FontMetrics fm = g2.getFontMetrics();
        int continueX = boardX + (boardWidth - fm.stringWidth(continueText)) / 2;
        g2.drawString(continueText, continueX, textY);

        g2.setComposite(originalComposite);
    }
}

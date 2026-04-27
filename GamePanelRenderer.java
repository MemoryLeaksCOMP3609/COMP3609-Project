import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

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

        int alpha = world.getOverlayAlpha();
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
        String title = "Bella's Nightmare";
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

        int backdropAlpha = Math.min(190, (int) (overlayAlpha * 255));
        g2.setColor(new Color(0, 0, 0, backdropAlpha));
        g2.fillRect(0, 0, panelWidth, panelHeight);

        int boardWidth = Math.min(760, Math.max(560, panelWidth - 120));
        int boardHeight = Math.min(640, Math.max(520, panelHeight - 100));
        int boardX = (panelWidth - boardWidth) / 2;
        int boardY = (panelHeight - boardHeight) / 2;
        int cornerRadius = 28;

        Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));

        Color panelFill = new Color(18, 18, 18, 235);
        Color panelBorder = Color.WHITE;
        Color accentColor = new Color(255, 220, 120);
        Color valueColor = new Color(255, 239, 196);
        Color secondaryTextColor = Color.LIGHT_GRAY;
        Color dividerColor = new Color(255, 255, 255, 70);
        Color chipFill = new Color(32, 32, 32, 220);

        g2.setColor(panelFill);
        g2.fillRoundRect(boardX, boardY, boardWidth, boardHeight, cornerRadius, cornerRadius);

        g2.setStroke(new java.awt.BasicStroke(3));
        g2.setColor(panelBorder);
        g2.drawRoundRect(boardX, boardY, boardWidth, boardHeight, cornerRadius, cornerRadius);

        int headerHeight = 70;
        g2.setColor(new Color(36, 20, 12, 185));
        g2.fillRoundRect(boardX + 10, boardY + 10, boardWidth - 20, headerHeight, 20, 20);
        g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 70));
        g2.fillRoundRect(boardX + 10, boardY + 10, boardWidth - 20, 16, 20, 20);

        Font titleFont = new Font("Arial", Font.BOLD, 30);
        Font sectionFont = new Font("Arial", Font.BOLD, 16);
        Font labelFont = new Font("Arial", Font.BOLD, 15);
        Font valueFont = new Font("Arial", Font.PLAIN, 15);
        Font detailFont = new Font("Arial", Font.PLAIN, 13);
        Font footerFont = new Font("Arial", Font.BOLD, 16);

        int sidePadding = 28;
        int contentX = boardX + sidePadding;
        int contentWidth = boardWidth - (sidePadding * 2);
        int sectionTop = boardY + headerHeight + 28;
        int leftColumnX = contentX;
        int rightColumnX = contentX + (contentWidth / 2) + 18;
        int columnWidth = (contentWidth / 2) - 18;
        int rowHeight = 26;
        int sectionGap = 20;

        g2.setFont(titleFont);
        g2.setColor(Color.WHITE);
        String title = "FINAL STATISTICS";
        int titleX = boardX + (boardWidth - g2.getFontMetrics().stringWidth(title)) / 2;
        int titleY = boardY + 34 + g2.getFontMetrics().getAscent() / 2;
        g2.drawString(title, titleX, titleY);

        g2.setFont(detailFont);
        g2.setColor(accentColor);
        String subtitle = "Your run at a glance";
        int subtitleX = boardX + (boardWidth - g2.getFontMetrics().stringWidth(subtitle)) / 2;
        g2.drawString(subtitle, subtitleX, boardY + 64);

        int leftY = drawScoreboardSection(g2, leftColumnX, sectionTop, columnWidth, "Progress", sectionFont,
                accentColor, dividerColor);
        leftY = drawScoreboardRow(g2, leftColumnX, leftY, columnWidth, "HP Accumulated",
                (stats.getHPAccumulated() >= 0 ? "+" : "") + stats.getHPAccumulated() + " HP",
                labelFont, valueFont, Color.WHITE, valueColor, chipFill, rowHeight);
        leftY = drawScoreboardRow(g2, leftColumnX, leftY, columnWidth, "Crystals Collected",
                String.valueOf(stats.getTotalCrystalsCollected()),
                labelFont, valueFont, Color.WHITE, valueColor, chipFill, rowHeight);
        leftY = drawScoreboardSubRow(g2, leftColumnX, leftY, columnWidth, "Tier 1",
                String.valueOf(stats.getCrystalCountByTier(DroppedCrystal.ExperienceTier.TIER_1)),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        leftY = drawScoreboardSubRow(g2, leftColumnX, leftY, columnWidth, "Tier 2",
                String.valueOf(stats.getCrystalCountByTier(DroppedCrystal.ExperienceTier.TIER_2)),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        leftY = drawScoreboardSubRow(g2, leftColumnX, leftY, columnWidth, "Tier 3",
                String.valueOf(stats.getCrystalCountByTier(DroppedCrystal.ExperienceTier.TIER_3)),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        leftY = drawScoreboardSubRow(g2, leftColumnX, leftY, columnWidth, "Tier 4",
                String.valueOf(stats.getCrystalCountByTier(DroppedCrystal.ExperienceTier.TIER_4)),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        leftY = drawScoreboardSubRow(g2, leftColumnX, leftY, columnWidth, "Health Crystals",
                String.valueOf(stats.getHealthCrystalsCollected()),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        leftY += 8;
        leftY = drawScoreboardRow(g2, leftColumnX, leftY, columnWidth, "Total Experience",
                stats.getTotalExperienceGained() + " XP",
                labelFont, valueFont, Color.WHITE, valueColor, chipFill, rowHeight);
        leftY = drawScoreboardRow(g2, leftColumnX, leftY, columnWidth, "Time Elapsed",
                stats.getFormattedTime(),
                labelFont, valueFont, Color.WHITE, valueColor, chipFill, rowHeight);

        int rightY = drawScoreboardSection(g2, rightColumnX, sectionTop, columnWidth, "Survival", sectionFont,
                accentColor, dividerColor);
        rightY = drawScoreboardRow(g2, rightColumnX, rightY, columnWidth, "Coins Collected",
                String.valueOf(stats.getCollectiblesCollected()),
                labelFont, valueFont, Color.WHITE, valueColor, chipFill, rowHeight);
        rightY += sectionGap;

        rightY = drawScoreboardSection(g2, rightColumnX, rightY, columnWidth, "Combat", sectionFont, accentColor,
                dividerColor);
        rightY = drawScoreboardRow(g2, rightColumnX, rightY, columnWidth, "Enemies Defeated",
                String.valueOf(stats.getTotalEnemiesDefeated()),
                labelFont, valueFont, Color.WHITE, valueColor, chipFill, rowHeight);
        rightY = drawScoreboardSubRow(g2, rightColumnX, rightY, columnWidth, "Bats",
                String.valueOf(stats.getEnemyDefeatCount("Bat")),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        rightY = drawScoreboardSubRow(g2, rightColumnX, rightY, columnWidth, "Normal Skeletons",
                String.valueOf(stats.getEnemyDefeatCount("NormalSkeleton")),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        rightY = drawScoreboardSubRow(g2, rightColumnX, rightY, columnWidth, "Normal Ghosts",
                String.valueOf(stats.getEnemyDefeatCount("NormalGhost")),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        rightY = drawScoreboardSubRow(g2, rightColumnX, rightY, columnWidth, "Toxic Skeletons",
                String.valueOf(stats.getEnemyDefeatCount("ToxicSkeleton")),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        rightY = drawScoreboardSubRow(g2, rightColumnX, rightY, columnWidth, "Dark Ghosts",
                String.valueOf(stats.getEnemyDefeatCount("DarkGhost")),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        rightY = drawScoreboardSubRow(g2, rightColumnX, rightY, columnWidth, "Frost Ghosts",
                String.valueOf(stats.getEnemyDefeatCount("FrostGhost")),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        rightY += sectionGap;

        rightY = drawScoreboardSection(g2, rightColumnX, rightY, columnWidth, "Bosses", sectionFont, accentColor,
                dividerColor);
        rightY = drawScoreboardRow(g2, rightColumnX, rightY, columnWidth, "Bosses Defeated",
                String.valueOf(stats.getTotalBossesDefeated()),
                labelFont, valueFont, Color.WHITE, valueColor, chipFill, rowHeight);
        rightY = drawScoreboardSubRow(g2, rightColumnX, rightY, columnWidth, "Phase One",
                String.valueOf(stats.getBossDefeatCount("BossPhaseOne")),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        rightY = drawScoreboardSubRow(g2, rightColumnX, rightY, columnWidth, "Phase Two",
                String.valueOf(stats.getBossDefeatCount("BossPhaseTwo")),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        rightY = drawScoreboardSubRow(g2, rightColumnX, rightY, columnWidth, "Phase Three",
                String.valueOf(stats.getBossDefeatCount("BossPhaseThree")),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        rightY = drawScoreboardSubRow(g2, rightColumnX, rightY, columnWidth, "Micro",
                String.valueOf(stats.getBossDefeatCount("BossPhaseThreeMicro")),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);
        rightY = drawScoreboardSubRow(g2, rightColumnX, rightY, columnWidth, "Mini",
                String.valueOf(stats.getBossDefeatCount("BossPhaseThreeMini")),
                detailFont, secondaryTextColor, accentColor, rowHeight - 6);

        int footerY = boardY + boardHeight - 46;
        g2.setColor(dividerColor);
        g2.drawLine(contentX, footerY - 18, contentX + contentWidth, footerY - 18);

        g2.setFont(footerFont);
        g2.setColor(accentColor);
        String continueText = "Press any key to close";
        FontMetrics footerMetrics = g2.getFontMetrics();
        int continueX = boardX + (boardWidth - footerMetrics.stringWidth(continueText)) / 2;
        g2.drawString(continueText, continueX, footerY);

        g2.setComposite(originalComposite);
    }

    private int drawScoreboardSection(Graphics2D g2, int x, int y, int width, String title, Font titleFont,
            Color titleColor, Color dividerColor) {
        g2.setFont(titleFont);
        g2.setColor(titleColor);
        g2.drawString(title, x, y);
        y += 18;
        g2.setColor(dividerColor);
        g2.drawLine(x, y, x + width, y);
        return y + 22;
    }

    private int drawScoreboardRow(Graphics2D g2, int x, int y, int width, String label, String value,
            Font labelFont, Font valueFont, Color labelColor, Color valueColor, Color chipFill, int rowHeight) {
        int chipWidth = 100;
        int chipHeight = 20;

        g2.setFont(labelFont);
        g2.setColor(labelColor);
        FontMetrics labelMetrics = g2.getFontMetrics();
        g2.drawString(label, x, y + labelMetrics.getAscent());

        int chipX = x + width - chipWidth;
        int chipY = y - 1;
        g2.setColor(chipFill);
        g2.fillRoundRect(chipX, chipY, chipWidth, chipHeight, 14, 14);

        g2.setFont(valueFont);
        g2.setColor(valueColor);
        FontMetrics valueMetrics = g2.getFontMetrics();
        int valueX = chipX + (chipWidth - valueMetrics.stringWidth(value)) / 2;
        int valueY = chipY + ((chipHeight - valueMetrics.getHeight()) / 2) + valueMetrics.getAscent();
        g2.drawString(value, valueX, valueY);
        return y + rowHeight;
    }

    private int drawScoreboardSubRow(Graphics2D g2, int x, int y, int width, String label, String value,
            Font font, Color textColor, Color accentColor, int rowHeight) {
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics();
        int baselineY = y + metrics.getAscent();

        g2.setColor(accentColor);
        g2.drawString("-", x + 8, baselineY);

        g2.setColor(textColor);
        g2.drawString(label, x + 24, baselineY);

        int valueX = x + width - metrics.stringWidth(value);
        g2.setColor(new Color(230, 230, 230));
        g2.drawString(value, valueX, baselineY);
        return y + rowHeight;
    }
}

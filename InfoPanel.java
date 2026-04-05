import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight HUD model/renderer for player stats in fullscreen mode.
 */
public class InfoPanel {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private String healthText;
    private String levelText;
    private String experienceText;
    private String speedText;
    private String damageText;
    private String fireRateText;
    private String regenText;
    private String fpsText;
    private String effectText;

    public InfoPanel() {
        healthText = "100 / 100";
        levelText = "1";
        experienceText = "0 / 100";
        speedText = "5";
        damageText = "1.00x";
        fireRateText = "1.00x";
        regenText = "1 / 5s";
        fpsText = "0";
        effectText = "None";
    }

    public void updatePlayerStats(Player player) {
        if (player == null) {
            return;
        }

        healthText = player.getHealth() + " / " + player.getMaxHealth();
        levelText = String.valueOf(player.getLevel());
        experienceText = player.isMaxLevel()
            ? "MAX"
            : player.getExperience() + " / " + player.getExperienceToNextLevel();
        speedText = String.valueOf(player.getMoveSpeed());
        damageText = DECIMAL_FORMAT.format(player.getDamageMultiplier()) + "x";
        fireRateText = DECIMAL_FORMAT.format(player.getFireRateMultiplier()) + "x";
        regenText = player.getHealthRegenPerInterval() + " / 5s";
    }

    public void updateFPS(int fps) {
        fpsText = String.valueOf(fps);
    }

    public void updateActiveEffects(String effectName) {
        if (effectName == null || effectName.isEmpty()) {
            effectText = "None";
        } else {
            effectText = effectName;
        }
    }

    public void drawHud(Graphics2D g2, int x, int y) {
        String[] lines = getHudLines();
        if (lines.length == 0) {
            return;
        }

        Font oldFont = g2.getFont();
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        int lineHeight = g2.getFontMetrics().getHeight();
        int contentWidth = 0;
        for (String line : lines) {
            contentWidth = Math.max(contentWidth, g2.getFontMetrics().stringWidth(line));
        }

        int boxWidth = contentWidth + 32;
        int boxHeight = 18 + (lineHeight * lines.length) + 12;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(x, y, boxWidth, boxHeight, 18, 18);
        g2.setColor(new Color(255, 255, 255, 210));
        g2.drawRoundRect(x, y, boxWidth, boxHeight, 18, 18);

        int textX = x + 16;
        int textY = y + 12 + g2.getFontMetrics().getAscent();
        g2.setColor(Color.WHITE);
        for (String line : lines) {
            g2.drawString(line, textX, textY);
            textY += lineHeight;
        }

        g2.setFont(oldFont);
    }

    public String[] getHudLines() {
        List<String> lines = new ArrayList<String>();
        lines.add("Health: " + healthText);
        lines.add("Level: " + levelText);
        lines.add("XP: " + experienceText);
        lines.add("Speed: " + speedText);
        lines.add("Damage: " + damageText);
        lines.add("Fire Rate: " + fireRateText);
        lines.add("Regen: " + regenText);
        lines.add("FPS: " + fpsText);
        lines.add("Effect: " + effectText);
        return lines.toArray(new String[0]);
    }
}

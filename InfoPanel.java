import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight HUD model/renderer for player stats in fullscreen mode.
 */
public class InfoPanel {
    private final List<String> hudLines;
    private String fpsText;

    public InfoPanel() {
        hudLines = new ArrayList<String>();
        fpsText = "0";
        buildDefaultHud();
    }

    public void updatePlayerStats(Player player) {
        if (player == null) {
            return;
        }

        hudLines.clear();
        hudLines.add("Health: " + player.getHealth() + " / " + player.getMaxHealth());
        hudLines.add("Level: " + player.getLevel());
        hudLines.add("XP: " + (player.isMaxLevel()
            ? "MAX"
            : player.getExperience() + " / " + player.getExperienceToNextLevel()));
        hudLines.add("Weapon: " + player.getWeaponType().getDisplayName());
        hudLines.add("Speed: " + player.getMoveSpeed());

        Weapon weapon = player.getWeapon();
        if (weapon != null) {
            weapon.appendHudStats(hudLines, player);
        }

        hudLines.add("Regen: " + player.getHealthRegenPerInterval() + " / 5s");
        hudLines.add("FPS: " + fpsText);
    }

    public void updateFPS(int fps) {
        fpsText = String.valueOf(fps);
        updateOrAppendFpsLine();
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
        return hudLines.toArray(new String[0]);
    }

    private void buildDefaultHud() {
        hudLines.clear();
        hudLines.add("Health: 100 / 100");
        hudLines.add("Level: 1");
        hudLines.add("XP: 0 / 100");
        hudLines.add("Weapon: Fire Arrow");
        hudLines.add("Speed: 5");
        hudLines.add("Damage: 1.00x");
        hudLines.add("Fire Rate: 1.00x");
        hudLines.add("Projectile Count: 1");
        hudLines.add("Projectile Size: 1.00x");
        hudLines.add("Regen: 1 / 5s");
        hudLines.add("FPS: " + fpsText);
    }

    private void updateOrAppendFpsLine() {
        String fpsLine = "FPS: " + fpsText;
        for (int i = 0; i < hudLines.size(); i++) {
            if (hudLines.get(i).startsWith("FPS: ")) {
                hudLines.set(i, fpsLine);
                return;
            }
        }

        hudLines.add(fpsLine);
    }
}

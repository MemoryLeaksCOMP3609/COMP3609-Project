import java.awt.*;
import java.text.DecimalFormat;
import javax.swing.*;

/**
 * Information display panel showing player stats and run state.
 */
public class InfoPanel extends JPanel {
    private final JTextField healthTF;
    private final JTextField levelTF;
    private final JTextField experienceTF;
    private final JTextField speedTF;
    private final JTextField damageTF;
    private final JTextField fireRateTF;
    private final JTextField fpsTF;
    private final JTextField effectsTF;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    public InfoPanel() {
        setLayout(new GridLayout(2, 8));
        setPreferredSize(new Dimension(800, 60));
        setBackground(Color.DARK_GRAY);

        addLabel("Health:");
        healthTF = addField(Color.PINK, "100 / 100");

        addLabel("Level:");
        levelTF = addField(Color.ORANGE, "1");

        addLabel("XP:");
        experienceTF = addField(Color.CYAN, "0 / 100");

        addLabel("Speed:");
        speedTF = addField(Color.YELLOW, "5");

        addLabel("Damage:");
        damageTF = addField(Color.GREEN, "1.00x");

        addLabel("Fire Rate:");
        fireRateTF = addField(new Color(180, 255, 180), "1.00x");

        addLabel("FPS:");
        fpsTF = addField(Color.LIGHT_GRAY, "0");

        addLabel("Effect:");
        effectsTF = addField(Color.WHITE, "None");
    }

    private void addLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        add(label);
    }

    private JTextField addField(Color background, String initialValue) {
        JTextField field = new JTextField(initialValue);
        field.setEditable(false);
        field.setForeground(Color.BLACK);
        field.setBackground(background);
        add(field);
        return field;
    }

    public void updatePlayerStats(Player player) {
        if (player == null) {
            return;
        }

        healthTF.setText(player.getHealth() + " / " + player.getMaxHealth());
        levelTF.setText(String.valueOf(player.getLevel()));
        experienceTF.setText(player.getExperience() + " / " + player.getExperienceToNextLevel());
        speedTF.setText(String.valueOf(player.getMoveSpeed()));
        damageTF.setText(DECIMAL_FORMAT.format(player.getDamageMultiplier()) + "x");
        fireRateTF.setText(DECIMAL_FORMAT.format(player.getFireRateMultiplier()) + "x");
    }

    public void updateFPS(int fps) {
        fpsTF.setText(String.valueOf(fps));
    }

    public void updateActiveEffects(String effectName) {
        if (effectName == null || effectName.isEmpty()) {
            effectsTF.setText("None");
        } else {
            effectsTF.setText(effectName);
        }
    }
}

import java.awt.*;
import javax.swing.*;

/**
 * Information display panel showing FPS, player coordinates,
 * active effects, and collectibles count.
 */
public class InfoPanel extends JPanel {
    
    private JLabel fpsLabel;
    private JLabel positionLabel;
    private JLabel effectsLabel;
    private JLabel collectiblesLabel;
    
    private JTextField fpsTF;
    private JTextField positionTF;
    private JTextField effectsTF;
    private JTextField collectiblesTF;
    
    public InfoPanel() {
        setLayout(new GridLayout(2, 4));
        setPreferredSize(new Dimension(800, 60));
        setBackground(Color.DARK_GRAY);
        
        // Create labels
        fpsLabel = new JLabel("FPS:");
        positionLabel = new JLabel("Position:");
        effectsLabel = new JLabel("Effect:");
        collectiblesLabel = new JLabel("Collectibles:");

        fpsLabel.setForeground(Color.WHITE);
        positionLabel.setForeground(Color.WHITE);
        effectsLabel.setForeground(Color.WHITE);
        collectiblesLabel.setForeground(Color.WHITE);

        
        // Create text fields
        fpsTF = new JTextField(5);
        positionTF = new JTextField(10);
        effectsTF = new JTextField(10);
        collectiblesTF = new JTextField(10);

        fpsTF.setForeground(Color.BLACK);
        positionTF.setForeground(Color.BLACK);
        effectsTF.setForeground(Color.BLACK);
        collectiblesTF.setForeground(Color.BLACK);

        
        // Make text fields non-editable
        fpsTF.setEditable(false);
        positionTF.setEditable(false);
        effectsTF.setEditable(false);
        collectiblesTF.setEditable(false);
        
        // Set colors
        fpsTF.setBackground(Color.CYAN);
        positionTF.setBackground(Color.YELLOW);
        effectsTF.setBackground(Color.GREEN);
        collectiblesTF.setBackground(Color.ORANGE);
        
        // Add to panel
        add(fpsLabel);
        add(fpsTF);
        add(positionLabel);
        add(positionTF);
        add(effectsLabel);
        add(effectsTF);
        add(collectiblesLabel);
        add(collectiblesTF);
        
        // Initialize values
        fpsTF.setText("0");
        positionTF.setText("(0, 0)");
        effectsTF.setText("None");
        collectiblesTF.setText("0 / 0");
    }
    
    public void updateFPS(int fps) {
        fpsTF.setText(String.valueOf(fps));
    }
    
    public void updatePlayerPosition(int worldX, int worldY) {
        positionTF.setText("(" + worldX + ", " + worldY + ")");
    }
    
    public void updateActiveEffects(String effectName) {
        if (effectName == null || effectName.isEmpty()) {
            effectsTF.setText("None");
        } else {
            effectsTF.setText(effectName);
        }
    }
    
    public void updateCollectibles(int collected, int total) {
        collectiblesTF.setText(collected + " / " + total);
    }
}

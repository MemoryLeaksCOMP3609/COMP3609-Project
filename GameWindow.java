import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Main game window frame that contains InfoPanel and GamePanel.
 * Handles keyboard input for player movement and game controls.
 */
public class GameWindow extends JFrame 
        implements ActionListener, KeyListener {
    
    // UI Components
    private Container c;
    private JPanel mainPanel;
    private JPanel buttonPanel;
    private GamePanel gamePanel;
    private InfoPanel infoPanel;
    
    // Buttons
    private JButton startB;
    private JButton pauseB;
    private JButton exitB;
    
    // Managers
    private SoundManager soundManager;
    
    public GameWindow() {
        setTitle("Coin Collector");
        setSize(800, 700);
        
        soundManager = SoundManager.getInstance();
        
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.BLACK);
        
        infoPanel = new InfoPanel();
        
        gamePanel = new GamePanel(infoPanel);
        
        createButtonPanel();
        
        // Add panels to main panel
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(gamePanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Set up keyboard input
        mainPanel.addKeyListener(this);
        
        // Add main panel to window
        c = getContentPane();
        c.add(mainPanel);
        
        // Set window properties
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }
    
    private void createButtonPanel() {
        buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.DARK_GRAY);
        
        startB = new JButton("Start");
        pauseB = new JButton("Pause");
        exitB = new JButton("Exit");
        
        startB.addActionListener(this);
        pauseB.addActionListener(this);
        exitB.addActionListener(this);
        
        buttonPanel.add(startB);
        buttonPanel.add(pauseB);
        buttonPanel.add(exitB);
    }
    
    private void updateInfoPanel() {
        PlayerSprite player = gamePanel.getPlayer();
        if (player != null) {
            infoPanel.updatePlayerPosition(player.getWorldX(), player.getWorldY());
        }
        infoPanel.updateFPS(gamePanel.getFPS());
        infoPanel.updateCollectibles(gamePanel.getCollectedCount(), gamePanel.getTotalCollectibles());
        infoPanel.updateActiveEffects(gamePanel.getActiveEffectName());
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        
        if (command.equals(startB.getText())) {
            if (gamePanel.isGameOver()) {
                gamePanel.resetGame();
            } else if (!gamePanel.isGameRunning()) {
                gamePanel.startGame();
            }
            mainPanel.requestFocus();
        }
        
        if (command.equals(pauseB.getText())) {
            gamePanel.pauseGame();
            if (gamePanel.isGamePaused()) {
                pauseB.setText("Resume");
            } else {
                pauseB.setText("Pause");
            }
            mainPanel.requestFocus();
        }
        
        if (command.equals(exitB.getText())) {
            System.exit(0);
        }
        
        mainPanel.requestFocus();
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        
        // Arrow keys
        if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
            gamePanel.setLeftKeyPressed(true);
        }
        if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
            gamePanel.setRightKeyPressed(true);
        }
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
            gamePanel.setUpKeyPressed(true);
        }
        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
            gamePanel.setDownKeyPressed(true);
        }
        
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        
        if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
            gamePanel.setLeftKeyPressed(false);
        }
        if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
            gamePanel.setRightKeyPressed(false);
        }
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
            gamePanel.setUpKeyPressed(false);
        }
        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
            gamePanel.setDownKeyPressed(false);
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
}

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Main game window frame that contains InfoPanel and GamePanel.
 * Handles keyboard input for player movement and game controls.
 */
public class GameWindow extends JFrame 
        implements ActionListener, KeyListener {
    private static final EnemySpawnType[] ENEMY_BUTTON_ORDER = {
        EnemySpawnType.BAT,
        EnemySpawnType.NORMAL_SKELETON,
        EnemySpawnType.TOXIC_SKELETON,
        EnemySpawnType.NORMAL_GHOST,
        EnemySpawnType.DARK_GHOST,
        EnemySpawnType.FROST_GHOST
    };
    
    // UI Components
    private Container c;
    private JPanel mainPanel;
    private JPanel buttonPanel;
    private JPanel enemyButtonPanel;
    private GamePanel gamePanel;
    private InfoPanel infoPanel;
    
    // Buttons
    private JButton startB;
    private JButton pauseB;
    private JButton exitB;
    private JToggleButton[] enemyTypeButtons;
    
    // Managers
    private SoundManager soundManager;
    
    public GameWindow() {
        setTitle("Coin Collector");
        setSize(800, 700);
        
        soundManager = SoundManager.getInstance();
        
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.BLACK);
        mainPanel.setFocusable(true);
        
        infoPanel = new InfoPanel();
        
        gamePanel = new GamePanel(infoPanel);
        gamePanel.setFocusable(true);
        
        createButtonPanel();
        
        // Add panels to main panel
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(gamePanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Set up keyboard input
        mainPanel.addKeyListener(this);
        gamePanel.addKeyListener(this);
        addKeyListener(this);
        
        // Add main panel to window
        c = getContentPane();
        c.add(mainPanel);
        
        // Set window properties
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        requestGameFocus();
    }
    
    private void createButtonPanel() {
        buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.DARK_GRAY);
        buttonPanel.setLayout(new BorderLayout());

        JPanel controlButtonPanel = new JPanel();
        controlButtonPanel.setBackground(Color.DARK_GRAY);
        
        startB = new JButton("Start");
        pauseB = new JButton("Pause");
        exitB = new JButton("Exit");
        
        startB.addActionListener(this);
        pauseB.addActionListener(this);
        exitB.addActionListener(this);
        
        controlButtonPanel.add(startB);
        controlButtonPanel.add(pauseB);
        controlButtonPanel.add(exitB);

        enemyButtonPanel = createEnemyButtonPanel();

        buttonPanel.add(controlButtonPanel, BorderLayout.NORTH);
        buttonPanel.add(enemyButtonPanel, BorderLayout.SOUTH);
    }

    private JPanel createEnemyButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        panel.setBackground(Color.DARK_GRAY);

        ButtonGroup enemyButtonGroup = new ButtonGroup();
        enemyTypeButtons = new JToggleButton[ENEMY_BUTTON_ORDER.length];

        for (int i = 0; i < ENEMY_BUTTON_ORDER.length; i++) {
            EnemySpawnType enemyType = ENEMY_BUTTON_ORDER[i];
            JToggleButton button = new JToggleButton(enemyType.getDisplayName());
            button.setActionCommand(enemyType.name());
            button.addActionListener(this);
            if (enemyType == gamePanel.getSelectedEnemyType()) {
                button.setSelected(true);
            }

            enemyButtonGroup.add(button);
            panel.add(button);
            enemyTypeButtons[i] = button;
        }

        return panel;
    }
    
    private void updateInfoPanel() {
        infoPanel.updatePlayerStats(gamePanel.getPlayerData());
        infoPanel.updateFPS(gamePanel.getFPS());
        infoPanel.updateActiveEffects(gamePanel.getActiveEffectName());
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        
        if (command.equals(startB.getText())) {
            if (gamePanel.isGameOver()) {
                WeaponType selectedWeapon = promptForWeaponChoice();
                if (selectedWeapon == null) {
                    requestGameFocus();
                    return;
                }
                gamePanel.setSelectedWeapon(selectedWeapon);
                gamePanel.resetGame();
            } else if (!gamePanel.isGameRunning()) {
                WeaponType selectedWeapon = promptForWeaponChoice();
                if (selectedWeapon == null) {
                    requestGameFocus();
                    return;
                }
                gamePanel.setSelectedWeapon(selectedWeapon);
                gamePanel.startGame();
            }
            requestGameFocus();
        }
        
        if (command.equals(pauseB.getText())) {
            gamePanel.pauseGame();
            if (gamePanel.isGamePaused()) {
                pauseB.setText("Resume");
            } else {
                pauseB.setText("Pause");
            }
            requestGameFocus();
        }
        
        if (command.equals(exitB.getText())) {
            System.exit(0);
        }

        for (EnemySpawnType enemyType : ENEMY_BUTTON_ORDER) {
            if (command.equals(enemyType.name())) {
                gamePanel.setSelectedEnemyType(enemyType);
                requestGameFocus();
                return;
            }
        }
        
        requestGameFocus();
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

    private WeaponType promptForWeaponChoice() {
        WeaponType[] options = WeaponType.values();
        String[] labels = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            labels[i] = options[i].getDisplayName();
        }

        int selectedIndex = JOptionPane.showOptionDialog(
            this,
            "Choose your starting weapon",
            "Weapon Select",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            labels,
            labels[0]
        );

        if (selectedIndex < 0 || selectedIndex >= options.length) {
            return null;
        }

        return options[selectedIndex];
    }

    private void requestGameFocus() {
        gamePanel.requestFocusInWindow();
        mainPanel.requestFocusInWindow();
        requestFocusInWindow();
    }
}

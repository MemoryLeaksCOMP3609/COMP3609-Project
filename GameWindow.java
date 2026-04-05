import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GameWindow extends JFrame implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
    private static final int NUM_BUFFERS = 2;
    private static final int TARGET_FPS = 60;
    private static final int FRAME_DELAY_MS = 1000 / TARGET_FPS;
    private static final Dimension LAUNCHER_SIZE = new Dimension(900, 650);
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 42;
    private static final int BUTTON_GAP = 12;
    private static final int BUTTON_MARGIN = 24;

    private final JPanel launcherPanel;
    private final JButton launcherStartButton;
    private final JButton launcherExitButton;
    private final InfoPanel infoPanel;
    private final GamePanel gamePanel;
    private final GraphicsDevice device;

    private BufferStrategy bufferStrategy;
    private BufferedImage screenImage;
    private Thread renderThread;
    private volatile boolean renderRunning;
    private boolean fullscreenActive;
    private boolean weaponSelectionActive;
    private Rectangle pauseButtonBounds;
    private Rectangle exitButtonBounds;
    private boolean mouseOverPauseButton;
    private boolean mouseOverExitButton;
    private Rectangle[] weaponOptionBounds;
    private Rectangle[] levelUpOptionBounds;
    private final WeaponType[] weaponOptions;
    private boolean[] weaponOptionHovered;
    private boolean[] levelUpOptionHovered;

    public GameWindow() {
        super("Coin Collector");

        device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        infoPanel = new InfoPanel();
        gamePanel = new GamePanel(infoPanel);
        fullscreenActive = false;
        renderThread = null;
        renderRunning = false;
        weaponSelectionActive = false;
        weaponOptions = WeaponType.values();
        weaponOptionBounds = new Rectangle[weaponOptions.length];
        weaponOptionHovered = new boolean[weaponOptions.length];
        levelUpOptionBounds = new Rectangle[3];
        levelUpOptionHovered = new boolean[3];

        launcherPanel = new JPanel();
        launcherPanel.setLayout(new BoxLayout(launcherPanel, BoxLayout.Y_AXIS));
        launcherPanel.setBackground(Color.BLACK);
        launcherPanel.setBorder(BorderFactory.createEmptyBorder(120, 120, 120, 120));

        JLabel titleLabel = new JLabel("Coin Collector");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 42));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Press Start to launch the game in fullscreen");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        subtitleLabel.setForeground(Color.LIGHT_GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        launcherStartButton = new JButton("Start Game");
        launcherStartButton.setActionCommand("LAUNCH_GAME");
        launcherStartButton.addActionListener(this);
        launcherStartButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        launcherExitButton = new JButton("Exit");
        launcherExitButton.setActionCommand("EXIT_LAUNCHER");
        launcherExitButton.addActionListener(this);
        launcherExitButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        launcherPanel.add(titleLabel);
        launcherPanel.add(Box.createVerticalStrut(16));
        launcherPanel.add(subtitleLabel);
        launcherPanel.add(Box.createVerticalStrut(32));
        launcherPanel.add(launcherStartButton);
        launcherPanel.add(Box.createVerticalStrut(12));
        launcherPanel.add(launcherExitButton);

        setLayout(new BorderLayout());
        add(launcherPanel, BorderLayout.CENTER);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setPreferredSize(LAUNCHER_SIZE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        launcherStartButton.requestFocusInWindow();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if ("LAUNCH_GAME".equals(command)) {
            launchGame();
            return;
        }

        if ("EXIT_LAUNCHER".equals(command)) {
            closeApplication();
        }
    }

    private void launchGame() {
        enterFullscreenMode();
        startRenderLoop();
        weaponSelectionActive = true;
        requestFocusInWindow();
    }

    private void enterFullscreenMode() {
        if (fullscreenActive) {
            return;
        }

        dispose();
        getContentPane().removeAll();
        setUndecorated(true);
        setIgnoreRepaint(true);
        setResizable(false);

        if (!device.isFullScreenSupported()) {
            throw new IllegalStateException("Full-screen exclusive mode is not supported on this system.");
        }

        device.setFullScreenWindow(this);
        setVisible(true);

        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        gamePanel.setViewportSize(width, height);
        screenImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        updateOverlayButtonBounds(width);

        try {
            createBufferStrategy(NUM_BUFFERS);
        } catch (IllegalStateException ex) {
            throw new IllegalStateException("Unable to create fullscreen buffer strategy.", ex);
        }

        bufferStrategy = getBufferStrategy();
        fullscreenActive = true;
    }

    private void startRenderLoop() {
        if (renderRunning) {
            return;
        }

        renderRunning = true;
        renderThread = new Thread(() -> {
            while (renderRunning) {
                try {
                    renderScreen();
                } catch (RuntimeException ex) {
                    System.err.println("Render loop crashed:");
                    ex.printStackTrace();
                    renderRunning = false;
                    break;
                }
                try {
                    Thread.sleep(FRAME_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            renderRunning = false;
        }, "RenderLoopThread");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    private void renderScreen() {
        if (!fullscreenActive || bufferStrategy == null) {
            return;
        }

        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        if (screenImage == null || screenImage.getWidth() != width || screenImage.getHeight() != height) {
            screenImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        gamePanel.setViewportSize(width, height);
        updateOverlayButtonBounds(width);
        updateChoiceBounds(width, height);

        do {
            do {
                Graphics2D imageGraphics = screenImage.createGraphics();
                try {
                    imageGraphics.setColor(Color.BLACK);
                    imageGraphics.fillRect(0, 0, width, height);
                    gamePanel.renderToScreen(imageGraphics, width, height);
                    drawHudOverlay(imageGraphics);
                    drawOverlayButtons(imageGraphics);
                    drawWeaponSelectionOverlay(imageGraphics, width, height);
                    drawLevelUpOverlay(imageGraphics, width, height);
                } finally {
                    imageGraphics.dispose();
                }

                Graphics graphics = bufferStrategy.getDrawGraphics();
                try {
                    graphics.drawImage(screenImage, 0, 0, width, height, null);
                } finally {
                    graphics.dispose();
                }
            } while (bufferStrategy.contentsRestored());

            if (!bufferStrategy.contentsLost()) {
                bufferStrategy.show();
            }
            Toolkit.getDefaultToolkit().sync();
        } while (bufferStrategy.contentsLost());
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_ESCAPE) {
            closeApplication();
            return;
        }

        if (!fullscreenActive) {
            return;
        }

        if (weaponSelectionActive || gamePanel.isLevelUpChoiceActive()) {
            return;
        }

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
        if (keyCode == KeyEvent.VK_P) {
            togglePause();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!fullscreenActive) {
            return;
        }

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
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!fullscreenActive) {
            return;
        }

        int mouseX = e.getX();
        int mouseY = e.getY();

        if (weaponSelectionActive) {
            handleWeaponSelectionClick(mouseX, mouseY);
            return;
        }

        if (gamePanel.isLevelUpChoiceActive()) {
            handleLevelUpClick(mouseX, mouseY);
            return;
        }

        if (pauseButtonBounds != null && pauseButtonBounds.contains(mouseX, mouseY)) {
            togglePause();
            return;
        }

        if (exitButtonBounds != null && exitButtonBounds.contains(mouseX, mouseY)) {
            closeApplication();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseOverPauseButton = false;
        mouseOverExitButton = false;
        clearChoiceHoverStates();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!fullscreenActive) {
            return;
        }

        int mouseX = e.getX();
        int mouseY = e.getY();
        mouseOverPauseButton = pauseButtonBounds != null && pauseButtonBounds.contains(mouseX, mouseY);
        mouseOverExitButton = exitButtonBounds != null && exitButtonBounds.contains(mouseX, mouseY);
        updateChoiceHoverStates(mouseX, mouseY);
    }

    private void togglePause() {
        if (!gamePanel.isGameRunning() || gamePanel.isGameOver()) {
            return;
        }

        gamePanel.pauseGame();
    }

    private void updateOverlayButtonBounds(int screenWidth) {
        int rightX = screenWidth - BUTTON_MARGIN - BUTTON_WIDTH;
        pauseButtonBounds = new Rectangle(
            rightX - BUTTON_WIDTH - BUTTON_GAP,
            BUTTON_MARGIN,
            BUTTON_WIDTH,
            BUTTON_HEIGHT
        );
        exitButtonBounds = new Rectangle(
            rightX,
            BUTTON_MARGIN,
            BUTTON_WIDTH,
            BUTTON_HEIGHT
        );
    }

    private void drawOverlayButtons(Graphics2D g2) {
        if (weaponSelectionActive || gamePanel.isLevelUpChoiceActive()) {
            return;
        }

        drawOverlayButton(
            g2,
            pauseButtonBounds,
            gamePanel.isGamePaused() ? "Resume" : "Pause",
            mouseOverPauseButton
        );
        drawOverlayButton(g2, exitButtonBounds, "Exit", mouseOverExitButton);
    }

    private void drawOverlayButton(Graphics2D g2, Rectangle bounds, String label, boolean hovered) {
        if (bounds == null) {
            return;
        }

        g2.setColor(new Color(0, 0, 0, hovered ? 210 : 170));
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 18, 18);

        g2.setColor(hovered ? new Color(255, 220, 120) : Color.WHITE);
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 18, 18);

        Font oldFont = g2.getFont();
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        int textWidth = g2.getFontMetrics().stringWidth(label);
        int textX = bounds.x + (bounds.width - textWidth) / 2;
        int textY = bounds.y + ((bounds.height - g2.getFontMetrics().getHeight()) / 2) + g2.getFontMetrics().getAscent();
        g2.drawString(label, textX, textY);
        g2.setFont(oldFont);
    }

    private void drawHudOverlay(Graphics2D g2) {
        infoPanel.drawHud(g2, 24, 24);
    }

    private void updateChoiceBounds(int screenWidth, int screenHeight) {
        int cardWidth = Math.min(320, screenWidth - 120);
        int cardHeight = 72;
        int gap = 18;
        int totalHeight = (cardHeight * 3) + (gap * 2);
        int startX = (screenWidth - cardWidth) / 2;
        int startY = (screenHeight - totalHeight) / 2 + 40;

        for (int i = 0; i < 3; i++) {
            levelUpOptionBounds[i] = new Rectangle(startX, startY + (i * (cardHeight + gap)), cardWidth, cardHeight);
        }

        int weaponWidth = Math.min(360, screenWidth - 120);
        int weaponHeight = 62;
        int weaponGap = 14;
        int weaponTotalHeight = (weaponHeight * weaponOptions.length) + (weaponGap * (weaponOptions.length - 1));
        int weaponStartX = (screenWidth - weaponWidth) / 2;
        int weaponStartY = (screenHeight - weaponTotalHeight) / 2 + 20;

        for (int i = 0; i < weaponOptions.length; i++) {
            weaponOptionBounds[i] = new Rectangle(
                weaponStartX,
                weaponStartY + (i * (weaponHeight + weaponGap)),
                weaponWidth,
                weaponHeight
            );
        }
    }

    private void drawWeaponSelectionOverlay(Graphics2D g2, int screenWidth, int screenHeight) {
        if (!weaponSelectionActive) {
            return;
        }

        drawModalBackdrop(g2, screenWidth, screenHeight);
        drawCenteredTitle(g2, "Choose Your Starting Weapon", screenWidth, screenHeight / 2 - 180, 34);

        Font oldFont = g2.getFont();
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        for (int i = 0; i < weaponOptions.length; i++) {
            drawChoiceButton(g2, weaponOptionBounds[i], weaponOptions[i].getDisplayName(), weaponOptionHovered[i]);
        }
        g2.setFont(oldFont);
    }

    private void drawLevelUpOverlay(Graphics2D g2, int screenWidth, int screenHeight) {
        if (!gamePanel.isLevelUpChoiceActive()) {
            return;
        }

        drawModalBackdrop(g2, screenWidth, screenHeight);
        drawCenteredTitle(g2, "Level Up", screenWidth, screenHeight / 2 - 170, 38);
        drawCenteredSubtitle(g2, gamePanel.getLevelUpPromptMessage(), screenWidth, screenHeight / 2 - 130);

        List<PlayerUpgradeOption> choices = gamePanel.getLevelUpChoices();
        for (int i = 0; i < choices.size() && i < levelUpOptionBounds.length; i++) {
            drawChoiceButton(
                g2,
                levelUpOptionBounds[i],
                choices.get(i).getDisplayName(gamePanel.getPlayerData()),
                levelUpOptionHovered[i]
            );
        }
    }

    private void drawModalBackdrop(Graphics2D g2, int screenWidth, int screenHeight) {
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, screenWidth, screenHeight);
    }

    private void drawCenteredTitle(Graphics2D g2, String text, int screenWidth, int y, int fontSize) {
        Font oldFont = g2.getFont();
        g2.setFont(new Font("Arial", Font.BOLD, fontSize));
        g2.setColor(Color.WHITE);
        int textWidth = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (screenWidth - textWidth) / 2, y);
        g2.setFont(oldFont);
    }

    private void drawCenteredSubtitle(Graphics2D g2, String text, int screenWidth, int y) {
        Font oldFont = g2.getFont();
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.setColor(Color.LIGHT_GRAY);
        int textWidth = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (screenWidth - textWidth) / 2, y);
        g2.setFont(oldFont);
    }

    private void drawChoiceButton(Graphics2D g2, Rectangle bounds, String label, boolean hovered) {
        if (bounds == null) {
            return;
        }

        g2.setColor(new Color(18, 18, 18, hovered ? 245 : 220));
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 22, 22);
        g2.setColor(hovered ? new Color(255, 220, 120) : Color.WHITE);
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 22, 22);

        Font oldFont = g2.getFont();
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        int textWidth = g2.getFontMetrics().stringWidth(label);
        int textX = bounds.x + (bounds.width - textWidth) / 2;
        int textY = bounds.y + ((bounds.height - g2.getFontMetrics().getHeight()) / 2) + g2.getFontMetrics().getAscent();
        g2.drawString(label, textX, textY);
        g2.setFont(oldFont);
    }

    private void handleWeaponSelectionClick(int mouseX, int mouseY) {
        for (int i = 0; i < weaponOptionBounds.length; i++) {
            if (weaponOptionBounds[i] != null && weaponOptionBounds[i].contains(mouseX, mouseY)) {
                weaponSelectionActive = false;
                clearChoiceHoverStates();
                gamePanel.setSelectedWeapon(weaponOptions[i]);
                gamePanel.startGame();
                return;
            }
        }
    }

    private void handleLevelUpClick(int mouseX, int mouseY) {
        List<PlayerUpgradeOption> choices = gamePanel.getLevelUpChoices();
        for (int i = 0; i < choices.size() && i < levelUpOptionBounds.length; i++) {
            if (levelUpOptionBounds[i] != null && levelUpOptionBounds[i].contains(mouseX, mouseY)) {
                clearChoiceHoverStates();
                gamePanel.applyLevelUpChoice(i);
                return;
            }
        }
    }

    private void updateChoiceHoverStates(int mouseX, int mouseY) {
        if (weaponSelectionActive) {
            for (int i = 0; i < weaponOptionBounds.length; i++) {
                weaponOptionHovered[i] = weaponOptionBounds[i] != null && weaponOptionBounds[i].contains(mouseX, mouseY);
            }
        } else {
            for (int i = 0; i < weaponOptionHovered.length; i++) {
                weaponOptionHovered[i] = false;
            }
        }

        if (gamePanel.isLevelUpChoiceActive()) {
            List<PlayerUpgradeOption> choices = gamePanel.getLevelUpChoices();
            for (int i = 0; i < levelUpOptionBounds.length; i++) {
                levelUpOptionHovered[i] = i < choices.size()
                    && levelUpOptionBounds[i] != null
                    && levelUpOptionBounds[i].contains(mouseX, mouseY);
            }
        } else {
            for (int i = 0; i < levelUpOptionHovered.length; i++) {
                levelUpOptionHovered[i] = false;
            }
        }
    }

    private void clearChoiceHoverStates() {
        for (int i = 0; i < weaponOptionHovered.length; i++) {
            weaponOptionHovered[i] = false;
        }

        for (int i = 0; i < levelUpOptionHovered.length; i++) {
            levelUpOptionHovered[i] = false;
        }
    }

    private void closeApplication() {
        renderRunning = false;
        if (renderThread != null) {
            renderThread.interrupt();
            if (Thread.currentThread() != renderThread) {
                try {
                    renderThread.join(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            renderThread = null;
        }

        gamePanel.stopGame();

        Window fullscreenWindow = device.getFullScreenWindow();
        if (fullscreenWindow == this) {
            device.setFullScreenWindow(null);
        }

        dispose();
        System.exit(0);
    }
}

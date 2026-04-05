import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;

public class LevelUpManager {
    private int pendingLevelUpChoices;
    private boolean levelUpDialogOpen;

    public LevelUpManager() {
        pendingLevelUpChoices = 0;
        levelUpDialogOpen = false;
    }

    public void reset() {
        pendingLevelUpChoices = 0;
        levelUpDialogOpen = false;
    }

    public void queueChoices(int levelsGained, GameSessionState sessionState) {
        if (levelsGained <= 0) {
            return;
        }

        pendingLevelUpChoices += levelsGained;
        sessionState.setActiveEffectName("Level Up");
    }

    public void processPendingChoices(JPanel panel, GameSessionState sessionState, GameWorld world,
                                      GameInputState inputState, SoundManager soundManager,
                                      InfoPanel infoPanel) {
        if (pendingLevelUpChoices <= 0 || levelUpDialogOpen || !sessionState.isGameRunning() || sessionState.isGameOver()) {
            return;
        }

        Player playerData = world.getPlayerData();
        if (playerData == null) {
            pendingLevelUpChoices = 0;
            return;
        }

        levelUpDialogOpen = true;
        boolean wasPaused = sessionState.isGamePaused();
        sessionState.setGamePaused(true);
        soundManager.stopClip("background");

        try {
            while (pendingLevelUpChoices > 0 && sessionState.isGameRunning() && !sessionState.isGameOver()) {
                PlayerUpgradeOption selectedUpgrade = promptForLevelUpChoice(panel, playerData);
                if (selectedUpgrade == null) {
                    continue;
                }

                selectedUpgrade.apply(playerData);
                pendingLevelUpChoices--;
                sessionState.setActiveEffectName(selectedUpgrade.getDisplayName());
                if (infoPanel != null) {
                    infoPanel.updatePlayerStats(playerData);
                }
            }
        } finally {
            levelUpDialogOpen = false;
            inputState.clearMovement();
            if (world.getPlayer() != null) {
                world.getPlayer().setIdle();
            }
            sessionState.setGamePaused(wasPaused);
            if (!sessionState.isGameOver() && sessionState.isGameRunning() && !sessionState.isGamePaused()) {
                soundManager.playBackgroundMusic();
            }
            panel.requestFocusInWindow();
        }
    }

    private PlayerUpgradeOption promptForLevelUpChoice(JPanel panel, Player playerData) {
        ArrayList<PlayerUpgradeOption> choices = new ArrayList<PlayerUpgradeOption>();
        for (PlayerUpgradeOption option : PlayerUpgradeOption.values()) {
            if (option.isAvailable(playerData)) {
                choices.add(option);
            }
        }
        Collections.shuffle(choices);

        int choiceCount = Math.min(3, choices.size());
        String[] labels = new String[choiceCount];
        for (int i = 0; i < choiceCount; i++) {
            labels[i] = choices.get(i).getDisplayName();
        }

        String title = "Level Up";
        String message = playerData.isMaxLevel()
            ? "You reached the max level."
            : "Level " + playerData.getLevel() + " - choose a stat to upgrade";

        while (true) {
            int selectedIndex = JOptionPane.showOptionDialog(
                SwingUtilities.getWindowAncestor(panel),
                message,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                labels,
                labels[0]
            );

            if (selectedIndex >= 0 && selectedIndex < choiceCount) {
                return choices.get(selectedIndex);
            }
        }
    }
}

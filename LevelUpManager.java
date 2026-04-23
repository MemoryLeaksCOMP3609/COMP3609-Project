import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LevelUpManager {
    private int pendingLevelUpChoices;
    private boolean levelUpDialogOpen;
    private boolean pausedBeforeDialog;
    private ArrayList<PlayerUpgradeOption> currentChoices;

    public LevelUpManager() {
        pendingLevelUpChoices = 0;
        levelUpDialogOpen = false;
        pausedBeforeDialog = false;
        currentChoices = new ArrayList<PlayerUpgradeOption>();
    }

    public void reset() {
        pendingLevelUpChoices = 0;
        levelUpDialogOpen = false;
        pausedBeforeDialog = false;
        currentChoices.clear();
    }

    public void queueChoices(int levelsGained, GameSessionState sessionState) {
        if (levelsGained <= 0) {
            return;
        }

        pendingLevelUpChoices += levelsGained;
    }

    public void processPendingChoices(GameSessionState sessionState, GameWorld world,
            GameInputState inputState, SoundManager soundManager) {
        if (pendingLevelUpChoices <= 0 || levelUpDialogOpen || !sessionState.isGameRunning()
                || sessionState.isGameOver()) {
            return;
        }

        Player playerData = world.getPlayerData();
        if (playerData == null) {
            pendingLevelUpChoices = 0;
            return;
        }

        prepareChoices(playerData, sessionState, inputState, soundManager, world);
    }

    public boolean isChoiceActive() {
        return levelUpDialogOpen;
    }

    public String getPromptMessage(Player playerData) {
        if (playerData == null) {
            return "Choose an upgrade";
        }

        if (playerData.isMaxLevel()) {
            return "You reached the max level.";
        }

        return "Level " + playerData.getLevel() + " - choose a stat to upgrade";
    }

    public List<PlayerUpgradeOption> getCurrentChoices() {
        return Collections.unmodifiableList(currentChoices);
    }

    public void applyChoice(int selectedIndex, GameSessionState sessionState, GameWorld world,
            GameInputState inputState, SoundManager soundManager,
            InfoPanel infoPanel) {
        if (!levelUpDialogOpen || selectedIndex < 0 || selectedIndex >= currentChoices.size()) {
            return;
        }

        Player playerData = world.getPlayerData();
        if (playerData == null) {
            closeChoiceSession(sessionState, world, inputState, soundManager);
            pendingLevelUpChoices = 0;
            return;
        }

        PlayerUpgradeOption selectedUpgrade = currentChoices.get(selectedIndex);
        selectedUpgrade.apply(playerData);
        pendingLevelUpChoices--;
        if (infoPanel != null) {
            infoPanel.updatePlayerStats(playerData);
        }

        currentChoices.clear();

        if (pendingLevelUpChoices > 0 && sessionState.isGameRunning() && !sessionState.isGameOver()) {
            prepareChoices(playerData, sessionState, inputState, soundManager, world);
            return;
        }

        closeChoiceSession(sessionState, world, inputState, soundManager);
    }

    private void prepareChoices(Player playerData, GameSessionState sessionState,
            GameInputState inputState, SoundManager soundManager,
            GameWorld world) {
        currentChoices.clear();
        for (PlayerUpgradeOption option : PlayerUpgradeOption.values()) {
            if (option.isAvailable(playerData)) {
                currentChoices.add(option);
            }
        }
        Collections.shuffle(currentChoices);

        while (currentChoices.size() > 3) {
            currentChoices.remove(currentChoices.size() - 1);
        }

        levelUpDialogOpen = true;
        pausedBeforeDialog = sessionState.isGamePaused();
        sessionState.setGamePaused(true);
        soundManager.stopClip("grass-background");
        soundManager.stopClip("sand-background");
        soundManager.stopClip("snow-background");
        inputState.clearMovement();
        if (world.getPlayer() != null) {
            world.getPlayer().setIdle();
        }
    }

    private void closeChoiceSession(GameSessionState sessionState, GameWorld world,
            GameInputState inputState, SoundManager soundManager) {
        levelUpDialogOpen = false;
        currentChoices.clear();
        inputState.clearMovement();
        if (world.getPlayer() != null) {
            world.getPlayer().setIdle();
        }
        sessionState.setGamePaused(pausedBeforeDialog);
        pausedBeforeDialog = false;
        if (!sessionState.isGameOver() && sessionState.isGameRunning() && !sessionState.isGamePaused()) {
            soundManager.playBackgroundMusic(world.getTerrainForCurrentLevel());
        }
    }
}

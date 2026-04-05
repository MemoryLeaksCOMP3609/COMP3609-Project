public class GameSessionState {
    private boolean gameRunning;
    private boolean gamePaused;
    private boolean gameOver;
    private int collectedCount;
    private int totalCollectibles;
    private int fps;
    private boolean goldenTintActive;
    private long goldenTintTimer;
    private long gameOverTime;
    private boolean gameExiting;

    public GameSessionState() {
        resetForNewPanel();
    }

    public void resetForNewPanel() {
        gameRunning = false;
        gamePaused = false;
        gameOver = false;
        collectedCount = 0;
        totalCollectibles = 0;
        fps = 0;
        goldenTintActive = false;
        goldenTintTimer = 0;
        gameOverTime = 0;
        gameExiting = false;
    }

    public void resetForNewGame() {
        gameRunning = true;
        gamePaused = false;
        gameOver = false;
        collectedCount = 0;
        goldenTintActive = false;
        goldenTintTimer = 0;
        gameOverTime = 0;
        gameExiting = false;
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public void setGameRunning(boolean gameRunning) {
        this.gameRunning = gameRunning;
    }

    public boolean isGamePaused() {
        return gamePaused;
    }

    public void setGamePaused(boolean gamePaused) {
        this.gamePaused = gamePaused;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public int getCollectedCount() {
        return collectedCount;
    }

    public void incrementCollectedCount() {
        collectedCount++;
    }

    public void setCollectedCount(int collectedCount) {
        this.collectedCount = collectedCount;
    }

    public int getTotalCollectibles() {
        return totalCollectibles;
    }

    public void setTotalCollectibles(int totalCollectibles) {
        this.totalCollectibles = totalCollectibles;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public boolean isGoldenTintActive() {
        return goldenTintActive;
    }

    public void setGoldenTintActive(boolean goldenTintActive) {
        this.goldenTintActive = goldenTintActive;
    }

    public long getGoldenTintTimer() {
        return goldenTintTimer;
    }

    public void setGoldenTintTimer(long goldenTintTimer) {
        this.goldenTintTimer = goldenTintTimer;
    }

    public long getGameOverTime() {
        return gameOverTime;
    }

    public void setGameOverTime(long gameOverTime) {
        this.gameOverTime = gameOverTime;
    }

    public boolean isGameExiting() {
        return gameExiting;
    }

    public void setGameExiting(boolean gameExiting) {
        this.gameExiting = gameExiting;
    }
}

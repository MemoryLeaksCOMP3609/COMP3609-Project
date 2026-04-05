import java.util.ArrayList;
import java.util.Random;

public class EnemySpawner {
    private static final int MAX_ACTIVE_SELECTED_ENEMIES = 3;
    private static final long SPAWN_COOLDOWN_MS = 1200;
    private static final int MIN_SPAWN_DISTANCE = 280;
    private static final int MAX_SPAWN_DISTANCE = 520;

    private final int worldWidth;
    private final int worldHeight;
    private final Random random;
    private EnemySpawnType selectedEnemyType;
    private long spawnCooldownRemaining;

    public EnemySpawner(int worldWidth, int worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.random = new Random();
        this.selectedEnemyType = EnemySpawnType.BAT;
        this.spawnCooldownRemaining = 0;
    }

    public void reset() {
        spawnCooldownRemaining = 0;
    }

    public void update(long deltaTimeMs, PlayerSprite player, ArrayList<Enemy> enemies) {
        if (player == null) {
            return;
        }

        if (spawnCooldownRemaining > 0) {
            spawnCooldownRemaining = Math.max(0, spawnCooldownRemaining - deltaTimeMs);
        }

        if (countLivingEnemiesOfSelectedType(enemies) >= MAX_ACTIVE_SELECTED_ENEMIES || spawnCooldownRemaining > 0) {
            return;
        }

        enemies.add(spawnEnemyNearPlayer(player));
        spawnCooldownRemaining = SPAWN_COOLDOWN_MS;
    }

    public void setSelectedEnemyType(EnemySpawnType selectedEnemyType) {
        if (selectedEnemyType == null) {
            return;
        }

        this.selectedEnemyType = selectedEnemyType;
        spawnCooldownRemaining = 0;
    }

    public EnemySpawnType getSelectedEnemyType() {
        return selectedEnemyType;
    }

    private int countLivingEnemiesOfSelectedType(ArrayList<Enemy> enemies) {
        int count = 0;
        for (Enemy enemy : enemies) {
            if (matchesSelectedType(enemy) && enemy.isAlive()) {
                count++;
            }
        }
        return count;
    }

    private boolean matchesSelectedType(Enemy enemy) {
        switch (selectedEnemyType) {
            case BAT:
                return enemy instanceof BatEnemy;
            case NORMAL_SKELETON:
                return enemy instanceof NormalSkeletonEnemy;
            case TOXIC_SKELETON:
                return enemy instanceof ToxicSkeletonEnemy;
            case NORMAL_GHOST:
                return enemy instanceof NormalGhostEnemy;
            case DARK_GHOST:
                return enemy instanceof DarkGhostEnemy;
            case FROST_GHOST:
                return enemy instanceof FrostGhostEnemy;
            default:
                return false;
        }
    }

    private Enemy spawnEnemyNearPlayer(PlayerSprite player) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = MIN_SPAWN_DISTANCE + random.nextInt(MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE + 1);
            int spawnX = clamp((int) Math.round(player.getWorldX() + Math.cos(angle) * distance), 0, worldWidth - 100);
            int spawnY = clamp((int) Math.round(player.getWorldY() + Math.sin(angle) * distance), 0, worldHeight - 100);
            return selectedEnemyType.createEnemy(spawnX, spawnY);
        }

        return selectedEnemyType.createEnemy(player.getWorldX() + MIN_SPAWN_DISTANCE, player.getWorldY());
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}

import java.util.ArrayList;
import java.util.Random;

public class EnemySpawner {
    private static final int MAX_ACTIVE_ENEMIES = 3;
    private static final long SPAWN_COOLDOWN_MS = 1200;
    private static final int MIN_SPAWN_DISTANCE = 280;
    private static final int MAX_SPAWN_DISTANCE = 520;

    private final int worldWidth;
    private final int worldHeight;
    private final Random random;

    private TestEnemySpawnType activeSpawnType;

    private long spawnCooldownRemaining;

    private boolean bossWasAlive = false;
    private Runnable onBossDefeatedCallback = null;

    @FunctionalInterface
    private interface EnemyFactory {
        Enemy create(int x, int y);
    }

    /** Grass (level 1) enemies */
    private static final EnemyFactory[] GRASS_ENEMIES = {
            (x, y) -> new BatEnemy(x, y),
            (x, y) -> new NormalSkeletonEnemy(x, y),
            (x, y) -> new NormalGhostEnemy(x, y),
    };

    /** Desert (level 2) enemies */
    private static final EnemyFactory[] DESERT_ENEMIES = {
            (x, y) -> new ToxicSkeletonEnemy(x, y),
            (x, y) -> new DarkGhostEnemy(x, y)
    };

    /** Ice (level 3) enemies */
    private static final EnemyFactory[] ICE_ENEMIES = {
            (x, y) -> new BatEnemy(x, y),
            (x, y) -> new ToxicSkeletonEnemy(x, y),
            (x, y) -> new FrostGhostEnemy(x, y),
    };

    public EnemySpawner(int worldWidth, int worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.random = new Random();
        this.activeSpawnType = null;
        this.spawnCooldownRemaining = 0;
    }

    public void reset() {
        activeSpawnType = null;
        spawnCooldownRemaining = 0;
        bossWasAlive = false;
    }

    public void setOnBossDefeatedCallback(Runnable callback) {
        this.onBossDefeatedCallback = callback;
    }

    public void setActiveSpawnType(TestEnemySpawnType activeSpawnType) {
        this.activeSpawnType = activeSpawnType;
        spawnCooldownRemaining = 0;
    }

    public TestEnemySpawnType getActiveSpawnType() {
        return activeSpawnType;
    }

    /**
     * Called every game tick
     *
     * @param deltaTimeMs  Time since last tick
     * @param player       Player sprite (used for spawn position)
     * @param enemies      Live enemy list (modified in place)
     * @param currentLevel 1 = grass, 2 = desert, 3 = ice
     */
    public void update(long deltaTimeMs, PlayerSprite player,
            ArrayList<Enemy> enemies, int currentLevel) {
        if (player == null)
            return;

        boolean bossAliveNow = hasLivingBoss(enemies);

        if (bossWasAlive && !bossAliveNow) {
            if (onBossDefeatedCallback != null) {
                onBossDefeatedCallback.run();
            }
        }
        bossWasAlive = bossAliveNow;

        if (bossAliveNow) {
            spawnCooldownRemaining = 0;
            return;
        }

        if (spawnCooldownRemaining > 0) {
            spawnCooldownRemaining = Math.max(0, spawnCooldownRemaining - deltaTimeMs);
        }

        if (spawnCooldownRemaining > 0)
            return;

        if (countLivingRegionEnemies(enemies, currentLevel) >= MAX_ACTIVE_ENEMIES)
            return;

        enemies.add(spawnEnemyNearPlayer(player, currentLevel));
        spawnCooldownRemaining = SPAWN_COOLDOWN_MS;
    }

    public void update(long deltaTimeMs, PlayerSprite player, ArrayList<Enemy> enemies) {
        update(deltaTimeMs, player, enemies, 1);
    }

    private boolean hasLivingBoss(ArrayList<Enemy> enemies) {
        for (Enemy enemy : enemies) {
            if (enemy instanceof BossEnemy && enemy.isAlive())
                return true;
        }
        return false;
    }

    private int countLivingRegionEnemies(ArrayList<Enemy> enemies, int currentLevel) {
        int count = 0;
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive())
                continue;
            if (activeSpawnType != null) {
                if (activeSpawnType.matches(enemy))
                    count++;
            } else {
                if (isRegionEnemy(enemy, currentLevel))
                    count++;
            }
        }
        return count;
    }

    private boolean isRegionEnemy(Enemy enemy, int currentLevel) {
        switch (currentLevel) {
            case 1:
                return enemy instanceof BatEnemy
                        || enemy instanceof NormalSkeletonEnemy
                        || enemy instanceof NormalGhostEnemy;
            case 2:
                return enemy instanceof ToxicSkeletonEnemy
                        || enemy instanceof DarkGhostEnemy;
            case 3:
                return enemy instanceof BatEnemy
                        || enemy instanceof ToxicSkeletonEnemy
                        || enemy instanceof FrostGhostEnemy;
            default:
                return false;
        }
    }

    private Enemy spawnEnemyNearPlayer(PlayerSprite player, int currentLevel) {
        if (activeSpawnType != null) {
            return spawnTestEnemy(player);
        }

        EnemyFactory[] pool = getRegionPool(currentLevel);

        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = MIN_SPAWN_DISTANCE
                    + random.nextInt(MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE + 1);
            int spawnX = clamp(
                    (int) Math.round(player.getWorldX() + Math.cos(angle) * distance),
                    0, worldWidth - 100);
            int spawnY = clamp(
                    (int) Math.round(player.getWorldY() + Math.sin(angle) * distance),
                    0, worldHeight - 100);

            EnemyFactory factory = pool[random.nextInt(pool.length)];
            return factory.create(spawnX, spawnY);
        }

        EnemyFactory factory = pool[random.nextInt(pool.length)];
        return factory.create(player.getWorldX() + MIN_SPAWN_DISTANCE, player.getWorldY());
    }

    private Enemy spawnTestEnemy(PlayerSprite player) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = MIN_SPAWN_DISTANCE
                    + random.nextInt(MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE + 1);
            int spawnX = clamp(
                    (int) Math.round(player.getWorldX() + Math.cos(angle) * distance),
                    0, worldWidth - 100);
            int spawnY = clamp(
                    (int) Math.round(player.getWorldY() + Math.sin(angle) * distance),
                    0, worldHeight - 100);
            return activeSpawnType.createEnemy(spawnX, spawnY);
        }
        return activeSpawnType.createEnemy(
                player.getWorldX() + MIN_SPAWN_DISTANCE, player.getWorldY());
    }

    private EnemyFactory[] getRegionPool(int currentLevel) {
        switch (currentLevel) {
            case 2:
                return DESERT_ENEMIES;
            case 3:
                return ICE_ENEMIES;
            default:
                return GRASS_ENEMIES;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
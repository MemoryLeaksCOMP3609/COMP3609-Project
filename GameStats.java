import java.util.HashMap;
import java.util.Map;

public class GameStats {
    private int basePlayerHealth;
    private int currentPlayerHealth;
    private int totalExperienceGained;
    private int heartsCollected;
    private long sessionStartTimeMs;
    private int speedBoostsActivated;

    private Map<DroppedCrystal.CrystalType, Map<DroppedCrystal.ExperienceTier, Integer>> crystalsByType;

    private Map<String, Integer> enemyDefeats;
    private Map<String, Integer> bossDefeats;

    public GameStats() {
        basePlayerHealth = 5000; // Player's Base HP
        currentPlayerHealth = basePlayerHealth;
        totalExperienceGained = 0;
        heartsCollected = 0;
        sessionStartTimeMs = System.currentTimeMillis();

        crystalsByType = new HashMap<>();
        crystalsByType.put(DroppedCrystal.CrystalType.EXPERIENCE, new HashMap<>());
        crystalsByType.put(DroppedCrystal.CrystalType.HEALTH, new HashMap<>());

        for (DroppedCrystal.ExperienceTier tier : DroppedCrystal.ExperienceTier.values()) {
            crystalsByType.get(DroppedCrystal.CrystalType.EXPERIENCE).put(tier, 0);
        }

        enemyDefeats = new HashMap<>();
        enemyDefeats.put("Bat", 0);
        enemyDefeats.put("NormalSkeleton", 0);
        enemyDefeats.put("NormalGhost", 0);
        enemyDefeats.put("ToxicSkeleton", 0);
        enemyDefeats.put("DarkGhost", 0);
        enemyDefeats.put("FrostGhost", 0);

        bossDefeats = new HashMap<>();
        bossDefeats.put("BossPhaseOne", 0);
        bossDefeats.put("BossPhaseTwo", 0);
        bossDefeats.put("BossPhaseThree", 0);
        bossDefeats.put("BossPhaseThreeMicro", 0);
        bossDefeats.put("BossPhaseThreeMini", 0);
    }

    public void resetSession() {
        basePlayerHealth = 5000;
        currentPlayerHealth = basePlayerHealth;
        totalExperienceGained = 0;
        heartsCollected = 0;
        speedBoostsActivated = 0;
        sessionStartTimeMs = System.currentTimeMillis();

        crystalsByType.get(DroppedCrystal.CrystalType.EXPERIENCE).clear();
        crystalsByType.get(DroppedCrystal.CrystalType.HEALTH).clear();

        for (DroppedCrystal.ExperienceTier tier : DroppedCrystal.ExperienceTier.values()) {
            crystalsByType.get(DroppedCrystal.CrystalType.EXPERIENCE).put(tier, 0);
        }

        for (String key : enemyDefeats.keySet()) {
            enemyDefeats.put(key, 0);
        }

        for (String key : bossDefeats.keySet()) {
            bossDefeats.put(key, 0);
        }
    }

    public void recordSpeedBoost() {
        speedBoostsActivated++;
    }

    public int getSpeedBoostsActivated() {
        return speedBoostsActivated;
    }

    public void recordCrystalCollected(DroppedCrystal.CrystalType type, DroppedCrystal.ExperienceTier tier) {
        if (type == null || tier == null)
            return;

        if (type == DroppedCrystal.CrystalType.EXPERIENCE) {
            Map<DroppedCrystal.ExperienceTier, Integer> tierCounts = crystalsByType.get(type);
            tierCounts.put(tier, tierCounts.getOrDefault(tier, 0) + 1);
        } else if (type == DroppedCrystal.CrystalType.HEALTH) {
            crystalsByType.get(type).put(null, crystalsByType.get(type).getOrDefault(null, 0) + 1);
        }
    }

    public void recordExperienceGained(int amount) {
        if (amount > 0) {
            totalExperienceGained += amount;
        }
    }

    public void recordHeartCollected() {
        heartsCollected++;
    }

    public void recordEnemyDefeated(Enemy enemy) {
        if (enemy == null)
            return;

        String enemyType = null;

        if (enemy instanceof BatEnemy) {
            enemyType = "Bat";
        } else if (enemy instanceof NormalSkeletonEnemy) {
            enemyType = "NormalSkeleton";
        } else if (enemy instanceof NormalGhostEnemy) {
            enemyType = "NormalGhost";
        } else if (enemy instanceof ToxicSkeletonEnemy) {
            enemyType = "ToxicSkeleton";
        } else if (enemy instanceof DarkGhostEnemy) {
            enemyType = "DarkGhost";
        } else if (enemy instanceof FrostGhostEnemy) {
            enemyType = "FrostGhost";
        }

        if (enemyType != null) {
            enemyDefeats.put(enemyType, enemyDefeats.getOrDefault(enemyType, 0) + 1);
        }
    }

    public void recordBossDefeated(BossEnemy boss) {
        if (boss == null)
            return;

        String bossType = null;

        if (boss instanceof BossPhaseOneEnemy) {
            bossType = "BossPhaseOne";
        } else if (boss instanceof BossPhaseTwoEnemy) {
            bossType = "BossPhaseTwo";
        } else if (boss instanceof BossPhaseThreeEnemy) {
            bossType = "BossPhaseThree";
        } else if (boss instanceof BossPhaseThreeMicroEnemy) {
            bossType = "BossPhaseThreeMicro";
        } else if (boss instanceof BossPhaseThreeMiniEnemy) {
            bossType = "BossPhaseThreeMini";
        }

        if (bossType != null) {
            bossDefeats.put(bossType, bossDefeats.getOrDefault(bossType, 0) + 1);
        }
    }

    public void setPlayerHealthData(int currentHealth, int maxHealth) {
        this.currentPlayerHealth = currentHealth;
        this.basePlayerHealth = maxHealth;
    }

    public int getHPAccumulated() {
        return currentPlayerHealth - basePlayerHealth;
    }

    public int getTotalCrystalsCollected() {
        int total = 0;
        for (Map<DroppedCrystal.ExperienceTier, Integer> tierCounts : crystalsByType.values()) {
            total += tierCounts.values().stream().mapToInt(Integer::intValue).sum();
        }
        return total;
    }

    public int getCrystalCountByTier(DroppedCrystal.ExperienceTier tier) {
        return crystalsByType.get(DroppedCrystal.CrystalType.EXPERIENCE).getOrDefault(tier, 0);
    }

    public int getHealthCrystalsCollected() {
        return crystalsByType.get(DroppedCrystal.CrystalType.HEALTH).getOrDefault(null, 0);
    }

    public int getTotalExperienceGained() {
        return totalExperienceGained;
    }

    public int getHeartsCollected() {
        return heartsCollected;
    }

    public int getTotalEnemiesDefeated() {
        return enemyDefeats.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getEnemyDefeatCount(String enemyType) {
        return enemyDefeats.getOrDefault(enemyType, 0);
    }

    public int getTotalBossesDefeated() {
        return bossDefeats.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getBossDefeatCount(String bossType) {
        return bossDefeats.getOrDefault(bossType, 0);
    }

    public long getTimeElapsedMs() {
        return System.currentTimeMillis() - sessionStartTimeMs;
    }

    public String getFormattedTime() {
        long totalSeconds = getTimeElapsedMs() / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%dm %ds", minutes, seconds);
    }

    public Map<String, Integer> getEnemyDefeats() {
        return new HashMap<>(enemyDefeats);
    }

    public Map<String, Integer> getBossDefeats() {
        return new HashMap<>(bossDefeats);
    }
}

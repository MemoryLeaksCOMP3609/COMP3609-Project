public enum TestBossSpawnType {
    PHASE_1("Boss P1") {
        @Override
        public BossEnemy createBoss(int spawnX, int spawnY) {
            return new BossPhaseOneEnemy(spawnX, spawnY);
        }
    },
    PHASE_2("Boss P2") {
        @Override
        public BossEnemy createBoss(int spawnX, int spawnY) {
            return new BossPhaseTwoEnemy(spawnX, spawnY);
        }
    },
    PHASE_3("Boss P3") {
        @Override
        public BossEnemy createBoss(int spawnX, int spawnY) {
            return new BossPhaseThreeEnemy(spawnX, spawnY);
        }
    };

    private final String displayName;

    TestBossSpawnType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public abstract BossEnemy createBoss(int spawnX, int spawnY);
}

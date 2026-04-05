public enum EnemySpawnType {
    BAT("Bat") {
        @Override
        public Enemy createEnemy(int startX, int startY) {
            return new BatEnemy(startX, startY);
        }
    },
    NORMAL_SKELETON("Normal Skeleton") {
        @Override
        public Enemy createEnemy(int startX, int startY) {
            return new NormalSkeletonEnemy(startX, startY);
        }
    },
    TOXIC_SKELETON("Toxic Skeleton") {
        @Override
        public Enemy createEnemy(int startX, int startY) {
            return new ToxicSkeletonEnemy(startX, startY);
        }
    },
    NORMAL_GHOST("Normal Ghost") {
        @Override
        public Enemy createEnemy(int startX, int startY) {
            return new NormalGhostEnemy(startX, startY);
        }
    },
    DARK_GHOST("Dark Ghost") {
        @Override
        public Enemy createEnemy(int startX, int startY) {
            return new DarkGhostEnemy(startX, startY);
        }
    },
    FROST_GHOST("Frost Ghost") {
        @Override
        public Enemy createEnemy(int startX, int startY) {
            return new FrostGhostEnemy(startX, startY);
        }
    },
    BOSS_PHASE_1("Boss Phase 1") {
        @Override
        public Enemy createEnemy(int startX, int startY) {
            return new BossPhaseOneEnemy(startX, startY);
        }
    },
    BOSS_PHASE_2("Boss Phase 2") {
        @Override
        public Enemy createEnemy(int startX, int startY) {
            return new BossPhaseTwoEnemy(startX, startY);
        }
    },
    BOSS_PHASE_3("Boss Phase 3") {
        @Override
        public Enemy createEnemy(int startX, int startY) {
            return new BossPhaseThreeEnemy(startX, startY);
        }
    };

    private final String displayName;

    EnemySpawnType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public abstract Enemy createEnemy(int startX, int startY);
}

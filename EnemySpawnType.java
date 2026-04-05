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

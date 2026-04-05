public enum TestEnemySpawnType {
    BAT("Bat") {
        @Override
        public Enemy createEnemy(int spawnX, int spawnY) {
            return new BatEnemy(spawnX, spawnY);
        }

        @Override
        public boolean matches(Enemy enemy) {
            return enemy instanceof BatEnemy;
        }
    },
    SKELETON("Skeleton") {
        @Override
        public Enemy createEnemy(int spawnX, int spawnY) {
            return new NormalSkeletonEnemy(spawnX, spawnY);
        }

        @Override
        public boolean matches(Enemy enemy) {
            return enemy instanceof NormalSkeletonEnemy;
        }
    },
    TOXIC_SKELETON("Toxic Skeleton") {
        @Override
        public Enemy createEnemy(int spawnX, int spawnY) {
            return new ToxicSkeletonEnemy(spawnX, spawnY);
        }

        @Override
        public boolean matches(Enemy enemy) {
            return enemy instanceof ToxicSkeletonEnemy;
        }
    },
    GHOST("Ghost") {
        @Override
        public Enemy createEnemy(int spawnX, int spawnY) {
            return new NormalGhostEnemy(spawnX, spawnY);
        }

        @Override
        public boolean matches(Enemy enemy) {
            return enemy instanceof NormalGhostEnemy;
        }
    },
    DARK_GHOST("Dark Ghost") {
        @Override
        public Enemy createEnemy(int spawnX, int spawnY) {
            return new DarkGhostEnemy(spawnX, spawnY);
        }

        @Override
        public boolean matches(Enemy enemy) {
            return enemy instanceof DarkGhostEnemy;
        }
    },
    FROST_GHOST("Frost Ghost") {
        @Override
        public Enemy createEnemy(int spawnX, int spawnY) {
            return new FrostGhostEnemy(spawnX, spawnY);
        }

        @Override
        public boolean matches(Enemy enemy) {
            return enemy instanceof FrostGhostEnemy;
        }
    };

    private final String displayName;

    TestEnemySpawnType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public abstract Enemy createEnemy(int spawnX, int spawnY);

    public abstract boolean matches(Enemy enemy);
}

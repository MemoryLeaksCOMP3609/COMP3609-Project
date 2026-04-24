public class BossPhaseThreeMiniEnemy extends BossEnemy {
    private static final long FRAME_DURATION = 140;

    public BossPhaseThreeMiniEnemy(int startX, int startY) {
        super("Abomination Mini", 3, 200, 8, 9, 0, 0, startX, startY);
        renderScale = 0.5;
        loadBossAnimations(
            "images/enemies/boss/bossPhase3Move.png",
            "images/enemies/boss/bossPhase3Attack.png",
            "images/enemies/boss/bossPhase3Death.png",
            FRAME_DURATION
        );
    }

    @Override
    public void useSpecialAttack() {
        attack();
    }

    @Override
    protected double getDashTriggerDistance() {
        return 500.0;
    }

    @Override
    protected double getMaxDashDistance() {
        return 1000.0;
    }

    @Override
    protected boolean supportsRangedAttack() {
        return true;
    }

    @Override
    protected boolean usesSplitDeathSequence() {
        return true;
    }

    @Override
    public void onRemoved(GameWorld world) {
        spawnChildrenAroundBody(world, ChildSpawnType.MICRO);
    }
}

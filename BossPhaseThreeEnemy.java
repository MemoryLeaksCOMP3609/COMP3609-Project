public class BossPhaseThreeEnemy extends BossEnemy {
    private static final long FRAME_DURATION = 140;

    public BossPhaseThreeEnemy(int startX, int startY) {
        super("Abomination Phase 3", 3, 800, 3, 35, 250, 250, startX, startY);
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
        return 1000.0;
    }

    @Override
    protected double getMaxDashDistance() {
        return 1500.0;
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
        spawnChildrenAroundBody(world, ChildSpawnType.MINI);
    }
}

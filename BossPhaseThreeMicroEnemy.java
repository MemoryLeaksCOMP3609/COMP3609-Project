public class BossPhaseThreeMicroEnemy extends BossEnemy {
    private static final long FRAME_DURATION = 140;

    public BossPhaseThreeMicroEnemy(int startX, int startY) {
        super("Abomination Micro", 3, 40, 9, 2, 0, 0, startX, startY);
        renderScale = 0.25;
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
        return 250.0;
    }

    @Override
    protected double getMaxDashDistance() {
        return 500.0;
    }

    @Override
    protected boolean usesSplitDeathSequence() {
        return true;
    }
}

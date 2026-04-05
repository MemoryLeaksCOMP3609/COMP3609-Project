public class BossPhaseOneEnemy extends BossEnemy {
    private static final long FRAME_DURATION = 140;

    public BossPhaseOneEnemy(int startX, int startY) {
        super("Abomination Phase 1", 1, 300, 2, 20, 100, 100, startX, startY);
        loadBossAnimations(
            "images/enemies/boss/bossPhase1Move.png",
            "images/enemies/boss/bossPhase1Attack.png",
            "images/enemies/boss/bossPhase1RunAway.png",
            FRAME_DURATION
        );
    }

    @Override
    public void useSpecialAttack() {
        attack();
    }

    @Override
    protected double getDashTriggerDistance() {
        return 600.0;
    }

    @Override
    protected double getMaxDashDistance() {
        return 750.0;
    }

    @Override
    protected boolean shouldRunAwayOnDeath() {
        return true;
    }
}

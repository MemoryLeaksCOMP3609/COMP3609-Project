public class BossPhaseTwoEnemy extends BossEnemy {
    private static final long FRAME_DURATION = 140;

    public BossPhaseTwoEnemy(int startX, int startY) {
        super("Abomination Phase 2", 2, 500, 3, 28, 150, 150, startX, startY);
        loadBossAnimations(
            "images/enemies/boss/bossPhase2Move.png",
            "images/enemies/boss/bossPhase2Attack.png",
            "images/enemies/boss/bossPhase2RunAway.png",
            FRAME_DURATION
        );
    }

    @Override
    public void useSpecialAttack() {
        attack();
    }

    @Override
    protected double getDashTriggerDistance() {
        return 800.0;
    }

    @Override
    protected double getMaxDashDistance() {
        return 1200.0;
    }

    @Override
    protected boolean supportsRangedAttack() {
        return true;
    }

    @Override
    protected boolean shouldRunAwayOnDeath() {
        return true;
    }
}

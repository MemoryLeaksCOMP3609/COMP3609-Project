public class BossPhaseThreeEnemy extends BossEnemy {
    private static final long FRAME_DURATION = 140;

    public BossPhaseThreeEnemy(int startX, int startY) {
        super("Abomination Phase 3", 3, 800, 3, 35, 250, 250, startX, startY);
        moveAnimation = loadStripAnimation("images/enemies/boss/bossPhase3Move.png", FRAME_DURATION, true);
        attackAnimation = loadStripAnimation("images/enemies/boss/bossPhase3Attack.png", FRAME_DURATION, true);
        deathAnimation = loadStripAnimation("images/enemies/boss/bossPhase3Death.png", FRAME_DURATION, false);
        idleAnimation = moveAnimation;
        setAnimationForState(EnemyState.MOVING);
    }

    @Override
    public void useSpecialAttack() {
        attack();
    }
}

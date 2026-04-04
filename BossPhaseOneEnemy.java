public class BossPhaseOneEnemy extends BossEnemy {
    private static final long FRAME_DURATION = 140;

    public BossPhaseOneEnemy(int startX, int startY) {
        super("Abomination Phase 1", 1, 300, 2, 20, 100, 100, startX, startY);
        moveAnimation = loadStripAnimation("images/enemies/boss/bossPhase1Move.png", FRAME_DURATION, true);
        attackAnimation = loadStripAnimation("images/enemies/boss/bossPhase1Attack.png", FRAME_DURATION, true);
        deathAnimation = loadStripAnimation("images/enemies/boss/bossPhase1RunAway.png", FRAME_DURATION, false);
        idleAnimation = moveAnimation;
        setAnimationForState(EnemyState.MOVING);
    }

    @Override
    public void useSpecialAttack() {
        attack();
    }
}

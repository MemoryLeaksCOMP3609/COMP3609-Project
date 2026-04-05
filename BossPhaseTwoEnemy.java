public class BossPhaseTwoEnemy extends BossEnemy {
    private static final long FRAME_DURATION = 140;

    public BossPhaseTwoEnemy(int startX, int startY) {
        super("Abomination Phase 2", 2, 500, 3, 28, 150, 150, startX, startY);
        moveAnimation = loadStripAnimation("images/enemies/boss/bossPhase2Move.png", FRAME_DURATION, true);
        attackAnimation = loadStripAnimation("images/enemies/boss/bossPhase2Attack.png", FRAME_DURATION, true);
        deathAnimation = loadStripAnimation("images/enemies/boss/bossPhase2RunAway.png", FRAME_DURATION, false);
        idleAnimation = moveAnimation;
        setAnimationForState(EnemyState.MOVING);
    }

    @Override
    public void useSpecialAttack() {
        attack();
    }
}

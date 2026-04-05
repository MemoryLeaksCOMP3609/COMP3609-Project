public abstract class BossEnemy extends Enemy {
    private static final long BOSS_ATTACK_COOLDOWN_MS = 3000L;
    private static final long BOSS_ATTACK_ANIMATION_MS = 500L;
    private static final double BOSS_DASH_SPEED_MULTIPLIER = 10.0;

    protected final int phaseNumber;
    private Animation attackSequenceAnimation;
    private boolean dashActive;
    private long attackAnimationRemainingMs;
    private boolean attackDamagePending;

    protected BossEnemy(String name, int phaseNumber, int maxHealth, int movementSpeed,
                        int contactDamage, int scoreValue, int experienceReward, int startX, int startY) {
        super(name, maxHealth, movementSpeed, contactDamage, scoreValue, experienceReward, startX, startY);
        this.phaseNumber = phaseNumber;
        this.spriteFacesLeftByDefault = false;
        this.attackSequenceAnimation = null;
        this.dashActive = false;
        this.attackAnimationRemainingMs = 0L;
        this.attackDamagePending = false;
    }

    public int getPhaseNumber() {
        return phaseNumber;
    }

    public void updateBehavior(PlayerSprite player, long deltaTimeMs) {
        if (player == null || !isAlive()) {
            return;
        }

        faceToward(player.getCenterX());

        if (dashActive) {
            if (PixelCollision.intersects(getBoundingRectangle(), getCurrentBufferedImage(),
                player.getBoundingRectangle(), player.getCurrentBufferedImage())) {
                dashActive = false;
                startAttackAnimation();
                return;
            }

            moveTowardWithSpeedMultiplier(player.getCenterX(), player.getCenterY(), 0, deltaTimeMs, BOSS_DASH_SPEED_MULTIPLIER);
            return;
        }

        if (attackAnimationRemainingMs > 0L) {
            advanceAttackAnimation(deltaTimeMs);
            if (attackAnimationRemainingMs == 0L) {
                setAnimationForState(EnemyState.MOVING);
            }
            return;
        }

        if (canAttack() && PixelCollision.intersects(getBoundingRectangle(), getCurrentBufferedImage(),
            player.getBoundingRectangle(), player.getCurrentBufferedImage())) {
            startAttackAnimation();
            return;
        }

        if (canAttack() && getDistanceToPlayer(player) <= getDashTriggerDistance()) {
            dashActive = true;
            setAnimationForState(EnemyState.MOVING);
            return;
        }

        moveToward(player.getCenterX(), player.getCenterY(), deltaTimeMs);
    }

    public boolean hasAttackDamagePending() {
        return attackDamagePending;
    }

    public void consumeAttackDamage() {
        attackDamagePending = false;
    }

    public boolean isOnAttackImpactFrame() {
        return attackDamagePending
            && currentAnimation == attackSequenceAnimation
            && attackSequenceAnimation != null
            && attackSequenceAnimation.getFrameCount() >= 3
            && attackSequenceAnimation.getCurrentFrameIndex() == 2;
    }

    protected void loadBossAnimations(String movePath, String attackPath, String deathPath, long frameDuration) {
        moveAnimation = loadStripAnimation(movePath, frameDuration, true);
        attackAnimation = buildAnimation(loadStripFrames(attackPath, 4),
            buildUniformDurations(4, BOSS_ATTACK_ANIMATION_MS), false);
        attackSequenceAnimation = attackAnimation;
        deathAnimation = loadStripAnimation(deathPath, frameDuration, false);
        idleAnimation = moveAnimation;
        setAnimationForState(EnemyState.MOVING);
    }

    private void startAttackAnimation() {
        useSpecialAttack();
        setAttackCooldown(BOSS_ATTACK_COOLDOWN_MS);
        attackAnimationRemainingMs = BOSS_ATTACK_ANIMATION_MS;
        dashActive = false;
        attackDamagePending = true;
        currentAnimation = attackSequenceAnimation != null ? attackSequenceAnimation : attackAnimation;
        if (currentAnimation != null) {
            currentAnimation.start();
        }
        state = EnemyState.ATTACKING;
    }

    private void advanceAttackAnimation(long deltaTimeMs) {
        attackAnimationRemainingMs = Math.max(0L, attackAnimationRemainingMs - deltaTimeMs);
        if (attackAnimationRemainingMs == 0L) {
            attackDamagePending = false;
        }
    }

    private double getDistanceToPlayer(PlayerSprite player) {
        double deltaX = player.getCenterX() - getCenterX();
        double deltaY = player.getCenterY() - getCenterY();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private long[] buildUniformDurations(int frameCount, long totalDurationMs) {
        long[] durations = new long[frameCount];
        if (frameCount <= 0) {
            return durations;
        }

        long baseDuration = totalDurationMs / frameCount;
        long remainder = totalDurationMs % frameCount;
        for (int i = 0; i < frameCount; i++) {
            durations[i] = baseDuration + (i < remainder ? 1L : 0L);
        }
        return durations;
    }

    protected abstract double getDashTriggerDistance();

    public abstract void useSpecialAttack();
}

import java.awt.image.BufferedImage;

public abstract class BossEnemy extends Enemy {
    private static final long BOSS_ATTACK_COOLDOWN_MS = 3000L;
    private static final long BOSS_ATTACK_ANIMATION_MS = 500L;
    private static final double BOSS_DASH_SPEED_MULTIPLIER = 10.0;
    private static final double BOSS_DEATH_MOVEMENT_REFERENCE_FRAME_MS = 40.0;
    private static final long RUN_AWAY_DEATH_DURATION_MS = 3000L;
    private static final double RUN_AWAY_SPEED_MULTIPLIER = 1.5;

    protected final int phaseNumber;
    private Animation attackSequenceAnimation;
    private boolean dashActive;
    private long attackAnimationRemainingMs;
    private boolean attackDamagePending;
    private double dashDistanceRemaining;
    private String deathAnimationPath;
    private long deathAnimationFrameDurationMs;
    private long deathElapsedMs;
    private double deathRunDirectionX;
    private double deathRunDirectionY;
    private int lastKnownPlayerCenterX;
    private int lastKnownPlayerCenterY;
    private int lastKnownPlayerMoveSpeed;

    protected BossEnemy(String name, int phaseNumber, int maxHealth, int movementSpeed,
                        int contactDamage, int scoreValue, int experienceReward, int startX, int startY) {
        super(name, maxHealth, movementSpeed, contactDamage, scoreValue, experienceReward, startX, startY);
        this.phaseNumber = phaseNumber;
        this.spriteFacesLeftByDefault = false;
        this.attackSequenceAnimation = null;
        this.dashActive = false;
        this.attackAnimationRemainingMs = 0L;
        this.attackDamagePending = false;
        this.dashDistanceRemaining = 0.0;
        this.deathAnimationPath = null;
        this.deathAnimationFrameDurationMs = 0L;
        this.deathElapsedMs = 0L;
        this.deathRunDirectionX = 0.0;
        this.deathRunDirectionY = -1.0;
        this.lastKnownPlayerCenterX = Integer.MIN_VALUE;
        this.lastKnownPlayerCenterY = Integer.MIN_VALUE;
        this.lastKnownPlayerMoveSpeed = 5;
    }

    public int getPhaseNumber() {
        return phaseNumber;
    }

    public void updateBehavior(PlayerSprite player, long deltaTimeMs) {
        if (player == null || !isAlive()) {
            return;
        }

        rememberPlayerState(player);
        faceToward(player.getCenterX());

        if (dashActive) {
            if (PixelCollision.intersects(getBoundingRectangle(), getCurrentBufferedImage(),
                player.getBoundingRectangle(), player.getCurrentBufferedImage())) {
                dashActive = false;
                dashDistanceRemaining = 0.0;
                startAttackAnimation();
                return;
            }

            double dashDistanceThisTick = movementSpeed * BOSS_DASH_SPEED_MULTIPLIER * (deltaTimeMs / 40.0);
            if (dashDistanceRemaining <= dashDistanceThisTick) {
                dashActive = false;
                dashDistanceRemaining = 0.0;
                setAnimationForState(EnemyState.MOVING);
                return;
            }

            moveTowardWithSpeedMultiplier(player.getCenterX(), player.getCenterY(), 0, deltaTimeMs, BOSS_DASH_SPEED_MULTIPLIER);
            dashDistanceRemaining = Math.max(0.0, dashDistanceRemaining - dashDistanceThisTick);
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
            dashDistanceRemaining = getMaxDashDistance();
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
        deathAnimationPath = deathPath;
        deathAnimationFrameDurationMs = frameDuration;
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

    @Override
    protected void onDeathStarted() {
        dashActive = false;
        dashDistanceRemaining = 0.0;
        attackAnimationRemainingMs = 0L;
        attackDamagePending = false;

        if (!shouldRunAwayOnDeath()) {
            return;
        }

        deathElapsedMs = 0L;
        if (deathAnimationPath != null) {
            deathAnimation = loadStripAnimation(deathAnimationPath, deathAnimationFrameDurationMs, true);
            currentAnimation = deathAnimation != null ? deathAnimation : currentAnimation;
            if (currentAnimation != null) {
                currentAnimation.start();
            }
        }

        double awayX = 0.0;
        double awayY = -1.0;
        if (lastKnownPlayerCenterX != Integer.MIN_VALUE && lastKnownPlayerCenterY != Integer.MIN_VALUE) {
            awayX = getCenterX() - lastKnownPlayerCenterX;
            awayY = getCenterY() - lastKnownPlayerCenterY;
            double distance = Math.sqrt(awayX * awayX + awayY * awayY);
            if (distance > 0.001) {
                deathRunDirectionX = awayX / distance;
                deathRunDirectionY = awayY / distance;
                updateFacingDirection(deathRunDirectionX);
                return;
            }
        }

        deathRunDirectionX = awayX;
        deathRunDirectionY = awayY;
    }

    @Override
    protected void updateDeath(long deltaTimeMs) {
        if (!shouldRunAwayOnDeath()) {
            super.updateDeath(deltaTimeMs);
            return;
        }

        deathElapsedMs = Math.min(RUN_AWAY_DEATH_DURATION_MS, deathElapsedMs + deltaTimeMs);
        double moveDistance = lastKnownPlayerMoveSpeed * RUN_AWAY_SPEED_MULTIPLIER
            * (deltaTimeMs / BOSS_DEATH_MOVEMENT_REFERENCE_FRAME_MS);
        worldX += (int) Math.round(deathRunDirectionX * moveDistance);
        worldY += (int) Math.round(deathRunDirectionY * moveDistance);

        if (currentAnimation != null) {
            currentAnimation.update(deltaTimeMs);
            syncDimensionsWithCurrentFrame();
        }

        double fadeProgress = (double) deathElapsedMs / RUN_AWAY_DEATH_DURATION_MS;
        renderWithAlpha((float) Math.max(0.0, 1.0 - fadeProgress));

        if (deathElapsedMs >= RUN_AWAY_DEATH_DURATION_MS) {
            renderWithAlpha(1.0f);
            state = EnemyState.DEAD;
        }
    }

    @Override
    public void draw(java.awt.Graphics2D g2) {
        BufferedImage frameToDraw = getFrameToDraw();
        if (frameToDraw == null) {
            return;
        }

        if (isDying() && shouldRunAwayOnDeath()) {
            double fadeProgress = (double) deathElapsedMs / RUN_AWAY_DEATH_DURATION_MS;
            drawFrame(g2, frameToDraw, (float) Math.max(0.0, 1.0 - fadeProgress));
            return;
        }

        drawFrame(g2, frameToDraw, 1.0f);
    }

    private void rememberPlayerState(PlayerSprite player) {
        lastKnownPlayerCenterX = player.getCenterX();
        lastKnownPlayerCenterY = player.getCenterY();
        if (player.getPlayerData() != null) {
            lastKnownPlayerMoveSpeed = player.getPlayerData().getMoveSpeed();
        }
    }

    private void renderWithAlpha(float alpha) {
        // Alpha is applied during draw; this method only preserves the current fade computation path.
    }

    protected abstract double getDashTriggerDistance();

    protected abstract double getMaxDashDistance();

    protected boolean shouldRunAwayOnDeath() {
        return false;
    }

    public abstract void useSpecialAttack();
}

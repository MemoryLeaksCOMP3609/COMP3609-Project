import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public abstract class BossEnemy extends Enemy {
    private static final long BOSS_ATTACK_COOLDOWN_MS = 3000L;
    private static final long BOSS_ATTACK_ANIMATION_MS = 500L;
    private static final double BOSS_DASH_SPEED_MULTIPLIER = 30.0;
    private static final double BOSS_DEATH_MOVEMENT_REFERENCE_FRAME_MS = 40.0;
    private static final long RUN_AWAY_DEATH_DURATION_MS = 3000L;
    private static final double RUN_AWAY_SPEED_MULTIPLIER = 1.5;
    private static final long SPLIT_DEATH_ANIMATION_MS = 1000L;
    private static final long SPLIT_DEATH_HOLD_MS = 1000L;
    private static final int SPLIT_CHILD_COUNT = 3;

    protected final int phaseNumber;
    private Animation attackSequenceAnimation;
    private Animation dashAnimation;
    private boolean dashActive;
    private long attackAnimationRemainingMs;
    private boolean attackDamagePending;
    private double dashDistanceRemaining;
    private double dashDirectionX;
    private double dashDirectionY;
    private long rangedAttackCooldownMs;
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
        this.dashAnimation = null;
        this.dashActive = false;
        this.attackAnimationRemainingMs = 0L;
        this.attackDamagePending = false;
        this.dashDistanceRemaining = 0.0;
        this.dashDirectionX = 0.0;
        this.dashDirectionY = 0.0;
        this.rangedAttackCooldownMs = 0L;
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
                dashDirectionX = 0.0;
                dashDirectionY = 0.0;
                startAttackAnimation();
                return;
            }

            double dashDistanceThisTick = movementSpeed * BOSS_DASH_SPEED_MULTIPLIER * (deltaTimeMs / 40.0);
            if (dashDistanceRemaining <= 0.0) {
                dashActive = false;
                dashDistanceRemaining = 0.0;
                dashDirectionX = 0.0;
                dashDirectionY = 0.0;
                setAnimationForState(EnemyState.MOVING);
                return;
            }

            double moveDistance = Math.min(dashDistanceRemaining, dashDistanceThisTick);
            updateFacingDirection(dashDirectionX);
            worldX += (int) Math.round(dashDirectionX * moveDistance);
            worldY += (int) Math.round(dashDirectionY * moveDistance);
            dashDistanceRemaining = Math.max(0.0, dashDistanceRemaining - moveDistance);

            if (dashDistanceRemaining <= 0.0) {
                dashActive = false;
                dashDirectionX = 0.0;
                dashDirectionY = 0.0;
                setAnimationForState(EnemyState.MOVING);
            }
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
            cacheDashDirection(player);
            setAttackCooldown(BOSS_ATTACK_COOLDOWN_MS);
            dashActive = true;
            dashDistanceRemaining = getMaxDashDistance();
            playDashAnimation();
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

    public void updateRangedAttackCooldown(long deltaTimeMs) {
        rangedAttackCooldownMs = Math.max(0L, rangedAttackCooldownMs - deltaTimeMs);
    }

    public boolean canUseRangedAttack() {
        return rangedAttackCooldownMs <= 0L;
    }

    public void setRangedAttackCooldown(long cooldownMs) {
        rangedAttackCooldownMs = cooldownMs;
    }

    public boolean isBusyWithMeleeAttack() {
        return dashActive || attackAnimationRemainingMs > 0L;
    }

    protected void loadBossAnimations(String movePath, String attackPath, String deathPath, long frameDuration) {
        moveAnimation = loadStripAnimation(movePath, frameDuration, true);
        BufferedImage[] attackFrames = loadStripFrames(attackPath, 4);
        attackAnimation = buildAnimation(attackFrames, buildUniformDurations(4, BOSS_ATTACK_ANIMATION_MS), false);
        attackSequenceAnimation = attackAnimation;
        loadDashAnimation(attackFrames);
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
        dashDirectionX = 0.0;
        dashDirectionY = 0.0;
        attackAnimationRemainingMs = 0L;
        attackDamagePending = false;

        if (usesSplitDeathSequence()) {
            deathElapsedMs = 0L;
            if (deathAnimationPath != null) {
                deathAnimation = buildSplitDeathAnimation(deathAnimationPath);
                currentAnimation = deathAnimation != null ? deathAnimation : currentAnimation;
                if (currentAnimation != null) {
                    currentAnimation.start();
                }
            }
            return;
        }

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
        if (usesSplitDeathSequence()) {
            deathElapsedMs = Math.min(SPLIT_DEATH_ANIMATION_MS + SPLIT_DEATH_HOLD_MS, deathElapsedMs + deltaTimeMs);
            if (currentAnimation != null) {
                currentAnimation.update(deltaTimeMs);
                syncDimensionsWithCurrentFrame();
            }

            if (deathElapsedMs >= SPLIT_DEATH_ANIMATION_MS + SPLIT_DEATH_HOLD_MS) {
                state = EnemyState.DEAD;
            }
            return;
        }

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

    protected boolean usesSplitDeathSequence() {
        return false;
    }

    protected void spawnChildrenAroundBody(GameWorld world, ChildSpawnType childSpawnType) {
        if (world == null) {
            return;
        }

        int centerX = getCenterX();
        int centerY = getCenterY();
        int spawnRadius = Math.max(60, width / 3);

        for (int i = 0; i < SPLIT_CHILD_COUNT; i++) {
            double angle = (-Math.PI / 2.0) + (i * (Math.PI * 2.0 / SPLIT_CHILD_COUNT));
            int spawnCenterX = (int) Math.round(centerX + Math.cos(angle) * spawnRadius);
            int spawnCenterY = (int) Math.round(centerY + Math.sin(angle) * spawnRadius);
            Enemy child = createChildEnemy(childSpawnType, spawnCenterX, spawnCenterY);
            if (child != null) {
                Rectangle2D.Double childBounds = child.getBoundingRectangle();
                child.worldX = spawnCenterX - (int) Math.round(childBounds.getWidth() / 2.0);
                child.worldY = spawnCenterY - (int) Math.round(childBounds.getHeight() / 2.0);
                world.getEnemies().add(child);
                // Track BossPhaseThreeMicroEnemy spawning
                if (child instanceof BossPhaseThreeMicroEnemy) {
                    world.trackBossPhaseThreeMicroSpawned();
                }
            }
        }
    }

    private void rememberPlayerState(PlayerSprite player) {
        lastKnownPlayerCenterX = player.getCenterX();
        lastKnownPlayerCenterY = player.getCenterY();
        if (player.getPlayerData() != null) {
            lastKnownPlayerMoveSpeed = player.getPlayerData().getMoveSpeed();
        }
    }

    private void cacheDashDirection(PlayerSprite player) {
        double deltaX = player.getCenterX() - getCenterX();
        double deltaY = player.getCenterY() - getCenterY();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (distance <= 0.001) {
            dashDirectionX = facingLeft ? -1.0 : 1.0;
            dashDirectionY = 0.0;
            return;
        }

        dashDirectionX = deltaX / distance;
        dashDirectionY = deltaY / distance;
    }

    private void loadDashAnimation(BufferedImage[] attackFrames) {
        if (attackFrames.length == 0) {
            dashAnimation = attackAnimation;
            return;
        }

        dashAnimation = new Animation(false);
        dashAnimation.addFrame(attackFrames[Math.min(1, attackFrames.length - 1)], BOSS_ATTACK_ANIMATION_MS);
    }

    private void playDashAnimation() {
        state = EnemyState.ATTACKING;
        currentAnimation = dashAnimation != null ? dashAnimation : attackAnimation;
        if (currentAnimation != null) {
            currentAnimation.start();
        }
    }

    private void renderWithAlpha(float alpha) {
        // Alpha is applied during draw; this method only preserves the current fade
        // computation path.
    }

    protected abstract double getDashTriggerDistance();

    protected abstract double getMaxDashDistance();

    protected boolean supportsRangedAttack() {
        return false;
    }

    protected boolean shouldRunAwayOnDeath() {
        return false;
    }

    private Animation buildSplitDeathAnimation(String deathPath) {
        BufferedImage[] frames = loadStripFrames(deathPath, 4);
        if (frames.length == 0) {
            return null;
        }

        long[] frameDurations = new long[frames.length + 1];
        BufferedImage[] heldFrames = new BufferedImage[frames.length + 1];
        long perFrameDuration = SPLIT_DEATH_ANIMATION_MS / frames.length;
        long remainder = SPLIT_DEATH_ANIMATION_MS % frames.length;
        for (int i = 0; i < frames.length; i++) {
            heldFrames[i] = frames[i];
            frameDurations[i] = perFrameDuration + (i < remainder ? 1L : 0L);
        }
        heldFrames[frames.length] = frames[frames.length - 1];
        frameDurations[frames.length] = SPLIT_DEATH_HOLD_MS;
        return buildAnimation(heldFrames, frameDurations, false);
    }

    private Enemy createChildEnemy(ChildSpawnType childSpawnType, int centerX, int centerY) {
        switch (childSpawnType) {
            case MINI:
                return new BossPhaseThreeMiniEnemy(centerX, centerY);
            case MICRO:
                return new BossPhaseThreeMicroEnemy(centerX, centerY);
            default:
                return null;
        }
    }

    protected enum ChildSpawnType {
        MINI,
        MICRO
    }

    public abstract void useSpecialAttack();
}

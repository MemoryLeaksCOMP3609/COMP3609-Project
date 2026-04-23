import java.awt.image.BufferedImage;

public abstract class SkeletonEnemy extends Enemy {
    private static final double ATTACK_TRIGGER_DISTANCE = 200.0;
    private static final long ATTACK_COOLDOWN_MS = 5000;
    private static final long WINDUP_DURATION_MS = 500;
    private static final long DASH_DURATION_MS = 120;
    private static final long RECOVERY_DURATION_MS = 120;
    private static final double DASH_DISTANCE = 300.0;
    private static final long[] DEATH_FRAME_DURATIONS_MS = {500L, 500L, 500L, 2000L};

    private Animation windupAnimation;
    private Animation dashAnimation;
    private Animation recoveryAnimation;
    private AttackPhase attackPhase;
    private long phaseElapsedMs;
    private long phaseDurationMs;
    private double dashStartX;
    private double dashStartY;
    private double dashDirectionX;
    private double dashDirectionY;
    private double dashTargetX;
    private double dashTargetY;
    private boolean dashHitApplied;

    protected SkeletonEnemy(String name, int maxHealth, int movementSpeed, int contactDamage,
                            int scoreValue, int experienceReward, int startX, int startY) {
        super(name, maxHealth, movementSpeed, contactDamage, scoreValue, experienceReward, startX, startY);
        spriteFacesLeftByDefault = false;
        attackPhase = AttackPhase.NONE;
        phaseElapsedMs = 0L;
        phaseDurationMs = 0L;
        dashHitApplied = false;
    }

    protected void loadSkeletonAnimations(String movePath, String attackPath, String deathPath, long frameDuration) {
        moveAnimation = loadStripAnimation(movePath, frameDuration, true);
        attackAnimation = loadStripAnimation(attackPath, frameDuration, true);
        deathAnimation = buildAnimation(loadStripFrames(deathPath, 4), DEATH_FRAME_DURATIONS_MS, false);
        idleAnimation = moveAnimation;
        loadAttackSequenceAnimations(attackPath);
        setAnimationForState(EnemyState.MOVING);
    }

    public void updateBehavior(PlayerSprite player, long deltaTimeMs) {
        if (player == null || !isAlive()) {
            return;
        }

        switch (attackPhase) {
            case NONE:
                faceToward(player.getCenterX());
                if (getDistanceTo(player) <= ATTACK_TRIGGER_DISTANCE && canAttack()) {
                    startWindup(player);
                } else {
                    moveToward(player.getCenterX(), player.getCenterY(), deltaTimeMs);
                }
                break;
            case WINDUP:
                updateFacingDirection(dashDirectionX);
                advancePhase(deltaTimeMs);
                if (phaseElapsedMs >= phaseDurationMs) {
                    startDash();
                }
                break;
            case DASH:
                updateFacingDirection(dashDirectionX);
                advanceDash(deltaTimeMs);
                break;
            case RECOVERY:
                faceToward(player.getCenterX());
                advancePhase(deltaTimeMs);
                if (phaseElapsedMs >= phaseDurationMs) {
                    finishAttack();
                }
                break;
            default:
                break;
        }
    }

    public boolean canDamagePlayerOnDash() {
        return attackPhase == AttackPhase.DASH && !dashHitApplied;
    }

    public void markDashHitApplied() {
        dashHitApplied = true;
    }

    private void loadAttackSequenceAnimations(String attackPath) {
        BufferedImage[] attackFrames = loadStripFrames(attackPath, 4);
        if (attackFrames.length == 0) {
            windupAnimation = attackAnimation;
            dashAnimation = attackAnimation;
            recoveryAnimation = attackAnimation;
            return;
        }

        windupAnimation = new Animation(false);
        windupAnimation.addFrame(attackFrames[0], WINDUP_DURATION_MS / 2);
        windupAnimation.addFrame(attackFrames[Math.min(1, attackFrames.length - 1)], WINDUP_DURATION_MS / 2);

        dashAnimation = new Animation(false);
        dashAnimation.addFrame(attackFrames[Math.min(2, attackFrames.length - 1)], DASH_DURATION_MS);

        recoveryAnimation = new Animation(false);
        recoveryAnimation.addFrame(attackFrames[Math.min(3, attackFrames.length - 1)], RECOVERY_DURATION_MS);
    }

    private void startWindup(PlayerSprite player) {
        attackPhase = AttackPhase.WINDUP;
        phaseElapsedMs = 0L;
        phaseDurationMs = WINDUP_DURATION_MS;
        dashHitApplied = false;
        setAttackCooldown(ATTACK_COOLDOWN_MS);
        cacheDashDirection(player);
        updateFacingDirection(dashDirectionX);
        playPhaseAnimation(windupAnimation);
    }

    private void startDash() {
        attackPhase = AttackPhase.DASH;
        phaseElapsedMs = 0L;
        phaseDurationMs = DASH_DURATION_MS;
        dashStartX = worldX;
        dashStartY = worldY;
        dashTargetX = dashStartX + dashDirectionX * DASH_DISTANCE;
        dashTargetY = dashStartY + dashDirectionY * DASH_DISTANCE;
        playPhaseAnimation(dashAnimation);
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

    private void startRecovery() {
        attackPhase = AttackPhase.RECOVERY;
        phaseElapsedMs = 0L;
        phaseDurationMs = RECOVERY_DURATION_MS;
        playPhaseAnimation(recoveryAnimation);
    }

    private void finishAttack() {
        attackPhase = AttackPhase.NONE;
        phaseElapsedMs = 0L;
        phaseDurationMs = 0L;
        setAnimationForState(EnemyState.MOVING);
    }

    private void advancePhase(long deltaTimeMs) {
        phaseElapsedMs = Math.min(phaseDurationMs, phaseElapsedMs + deltaTimeMs);
    }

    private void advanceDash(long deltaTimeMs) {
        advancePhase(deltaTimeMs);
        double progress = phaseDurationMs <= 0 ? 1.0 : (double) phaseElapsedMs / phaseDurationMs;
        worldX = (int) Math.round(dashStartX + (dashTargetX - dashStartX) * progress);
        worldY = (int) Math.round(dashStartY + (dashTargetY - dashStartY) * progress);

        if (phaseElapsedMs >= phaseDurationMs) {
            startRecovery();
        }
    }

    private void playPhaseAnimation(Animation animation) {
        state = EnemyState.ATTACKING;
        currentAnimation = animation != null ? animation : attackAnimation;
        if (currentAnimation != null) {
            currentAnimation.start();
        }
    }

    private double getDistanceTo(PlayerSprite player) {
        int deltaX = player.getCenterX() - getCenterX();
        int deltaY = player.getCenterY() - getCenterY();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private enum AttackPhase {
        NONE,
        WINDUP,
        DASH,
        RECOVERY
    }
}

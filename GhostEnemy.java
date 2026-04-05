import java.awt.Graphics2D;
import java.util.Random;

public abstract class GhostEnemy extends Enemy {
    private static final long FADE_OUT_DURATION_MS = 3000L;
    private static final long INVISIBLE_DURATION_MS = 1000L;
    private static final long FADE_IN_DURATION_MS = 1000L;
    private static final long VISIBLE_DURATION_MS = 1000L;

    protected final double transparency;
    private final Random random;
    private GhostPhase phase;
    private long phaseElapsedMs;
    private double teleportRadius;
    private float currentOpacity;

    protected GhostEnemy(String name, int maxHealth, int movementSpeed, int contactDamage,
                         int scoreValue, int experienceReward, double transparency,
                         int startX, int startY) {
        super(name, maxHealth, movementSpeed, contactDamage, scoreValue, experienceReward, startX, startY);
        this.transparency = transparency;
        this.random = new Random();
        this.phase = GhostPhase.FADING_OUT;
        this.phaseElapsedMs = 0L;
        this.teleportRadius = 0.0;
        this.currentOpacity = (float) transparency;
    }

    public double getTransparency() {
        return transparency;
    }

    public void updateBehavior(PlayerSprite player, long deltaTimeMs, int worldWidth, int worldHeight) {
        if (player == null || isDead()) {
            return;
        }

        faceToward(player.getCenterX());
        phaseElapsedMs += deltaTimeMs;

        switch (phase) {
            case FADING_OUT:
                moveToward(player.getCenterX(), player.getCenterY(), deltaTimeMs);
                currentOpacity = (float) (transparency * Math.max(0.0, 1.0 - (double) phaseElapsedMs / FADE_OUT_DURATION_MS));
                if (phaseElapsedMs >= FADE_OUT_DURATION_MS) {
                    teleportRadius = getDistanceToPlayer(player);
                    phase = GhostPhase.INVISIBLE;
                    phaseElapsedMs = 0L;
                    currentOpacity = 0.0f;
                    setAnimationForState(EnemyState.IDLE);
                }
                break;
            case INVISIBLE:
                currentOpacity = 0.0f;
                setAnimationForState(EnemyState.IDLE);
                if (phaseElapsedMs >= INVISIBLE_DURATION_MS) {
                    repositionAroundPlayer(player, worldWidth, worldHeight);
                    faceToward(player.getCenterX());
                    phase = GhostPhase.FADING_IN;
                    phaseElapsedMs = 0L;
                    setAnimationForState(EnemyState.MOVING);
                }
                break;
            case FADING_IN:
                moveToward(player.getCenterX(), player.getCenterY(), deltaTimeMs);
                currentOpacity = (float) (transparency * Math.min(1.0, (double) phaseElapsedMs / FADE_IN_DURATION_MS));
                if (phaseElapsedMs >= FADE_IN_DURATION_MS) {
                    phase = GhostPhase.VISIBLE;
                    phaseElapsedMs = 0L;
                    currentOpacity = (float) transparency;
                    setAnimationForState(EnemyState.MOVING);
                }
                break;
            case VISIBLE:
                currentOpacity = (float) transparency;
                moveToward(player.getCenterX(), player.getCenterY(), deltaTimeMs);
                if (phaseElapsedMs >= VISIBLE_DURATION_MS) {
                    phase = GhostPhase.FADING_OUT;
                    phaseElapsedMs = 0L;
                    setAnimationForState(EnemyState.MOVING);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean isTargetable() {
        return super.isTargetable() && phase != GhostPhase.INVISIBLE;
    }

    @Override
    public boolean canTakeDamage() {
        return super.canTakeDamage() && phase != GhostPhase.INVISIBLE;
    }

    @Override
    public void draw(Graphics2D g2) {
        drawFrame(g2, getFrameToDraw(), currentOpacity);
    }

    private double getDistanceToPlayer(PlayerSprite player) {
        double deltaX = player.getCenterX() - getCenterX();
        double deltaY = player.getCenterY() - getCenterY();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private void repositionAroundPlayer(PlayerSprite player, int worldWidth, int worldHeight) {
        syncDimensionsWithCurrentFrame();

        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int nextCenterX = (int) Math.round(player.getCenterX() + Math.cos(angle) * teleportRadius);
            int nextCenterY = (int) Math.round(player.getCenterY() + Math.sin(angle) * teleportRadius);
            int nextWorldX = clamp(nextCenterX - width / 2, 0, Math.max(0, worldWidth - width));
            int nextWorldY = clamp(nextCenterY - height / 2, 0, Math.max(0, worldHeight - height));
            double distanceFromPlayer = Math.sqrt(
                Math.pow(nextWorldX + width / 2.0 - player.getCenterX(), 2)
                + Math.pow(nextWorldY + height / 2.0 - player.getCenterY(), 2)
            );

            if (Math.abs(distanceFromPlayer - teleportRadius) <= 16.0 || attempt == 19) {
                worldX = nextWorldX;
                worldY = nextWorldY;
                return;
            }
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private enum GhostPhase {
        FADING_OUT,
        INVISIBLE,
        FADING_IN,
        VISIBLE
    }
}

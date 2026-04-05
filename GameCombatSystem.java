import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.function.IntConsumer;

public class GameCombatSystem {
    private static final int HEALTH_PER_CRYSTAL = 20;

    private final GameWorld world;
    private final GameSessionState sessionState;
    private final SoundManager soundManager;
    private final long goldenTintDurationMs;
    private final long batFireIntervalMs;
    private final int experiencePerCollectible;
    private final int batStopDistance;
    private final double batBulletSpeed;

    public GameCombatSystem(GameWorld world, GameSessionState sessionState, SoundManager soundManager,
                            long goldenTintDurationMs, long batFireIntervalMs,
                            int experiencePerCollectible, int batStopDistance,
                            double batBulletSpeed) {
        this.world = world;
        this.sessionState = sessionState;
        this.soundManager = soundManager;
        this.goldenTintDurationMs = goldenTintDurationMs;
        this.batFireIntervalMs = batFireIntervalMs;
        this.experiencePerCollectible = experiencePerCollectible;
        this.batStopDistance = batStopDistance;
        this.batBulletSpeed = batBulletSpeed;
    }

    public void updateEnemies(long deltaTimeMs, PlayerSprite player) {
        if (player == null) {
            return;
        }

        world.getEnemySpawner().update(deltaTimeMs, player, world.getEnemies());

        Iterator<Enemy> enemyIterator = world.getEnemies().iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            if (enemy.isDead()) {
                enemyIterator.remove();
                continue;
            }

            enemy.updateDamageFlash(deltaTimeMs);
            enemy.updateAttackCooldown(deltaTimeMs);
            enemy.update(deltaTimeMs);
            double distanceToPlayer = getDistance(enemy.getCenterX(), enemy.getCenterY(), player.getCenterX(), player.getCenterY());

            if (enemy instanceof BatEnemy) {
                updateBatEnemy(enemy, player, deltaTimeMs, distanceToPlayer);
            } else {
                enemy.moveToward(player.getCenterX(), player.getCenterY(), deltaTimeMs);
            }
        }
    }

    private void updateBatEnemy(Enemy enemy, PlayerSprite player, long deltaTimeMs, double distanceToPlayer) {
        if (distanceToPlayer > batStopDistance) {
            enemy.moveToward(player.getCenterX(), player.getCenterY(), batStopDistance, deltaTimeMs);
            return;
        }

        enemy.attack();
        if (enemy.canAttack()) {
            world.getProjectiles().add(createProjectile(enemy.getCenterX(), enemy.getCenterY(),
                player.getCenterX(), player.getCenterY(), batBulletSpeed,
                enemy.getContactDamage(), true, "images/spells/waterArrow", 0.10,
                WeaponType.FIRE_ARROW, Projectile.MotionMode.STRAIGHT));
            enemy.setAttackCooldown(batFireIntervalMs);
        }
    }

    public void updateProjectiles(long deltaTimeMs, PlayerSprite player) {
        Iterator<Projectile> projectileIterator = world.getProjectiles().iterator();
        while (projectileIterator.hasNext()) {
            Projectile projectile = projectileIterator.next();
            projectile.update(deltaTimeMs, world, player);
            if (!projectile.isActive() || projectile.isOutOfBounds(world.getWorldWidth(), world.getWorldHeight())) {
                projectileIterator.remove();
            }
        }
    }

    public void checkCollisions(PlayerSprite player, Player playerData,
                                IntConsumer queueLevelUpChoices, Runnable onPlayerDeath) {
        if (player == null || !sessionState.isGameRunning() || sessionState.isGamePaused()) {
            return;
        }

        Rectangle2D.Double playerBounds = player.getBoundingRectangle();
        handleCollectibleCollisions(player, playerData, playerBounds, queueLevelUpChoices);
        handleCrystalCollisions(playerData, playerBounds, queueLevelUpChoices);
        handleProjectileCollisions(player, playerData, onPlayerDeath);
    }

    private void handleCollectibleCollisions(PlayerSprite player, Player playerData,
                                             Rectangle2D.Double playerBounds,
                                             IntConsumer queueLevelUpChoices) {
        for (Collectible collectible : world.getCollectibles()) {
            if (!collectible.isCollected()
                && PixelCollision.intersects(playerBounds, player.getCurrentBufferedImage(),
                collectible.getBoundingRectangle(), collectible.getCurrentBufferedImage())) {
                collectible.collect();
                sessionState.incrementCollectedCount();
                soundManager.playClip("coinPickup", false);
                player.activateSpeedBoost();
                sessionState.setGoldenTintActive(true);
                sessionState.setGoldenTintTimer(goldenTintDurationMs);
                sessionState.setActiveEffectName("Golden Tint");
                queueLevelUpChoices.accept(playerData != null ? playerData.gainExperience(experiencePerCollectible) : 0);
                world.respawnCollectedCollectible(collectible);
                sessionState.setTotalCollectibles(world.getCollectibles().size());
                break;
            }
        }
    }

    private void handleCrystalCollisions(Player playerData, Rectangle2D.Double playerBounds,
                                         IntConsumer queueLevelUpChoices) {
        Iterator<DroppedCrystal> crystalIterator = world.getDroppedCrystals().iterator();
        while (crystalIterator.hasNext()) {
            DroppedCrystal crystal = crystalIterator.next();
            if (!crystal.isCollected() && playerBounds.intersects(crystal.getBoundingRectangle())) {
                crystal.collect();
                if (playerData != null) {
                    if (crystal.getType() == DroppedCrystal.CrystalType.EXPERIENCE) {
                        queueLevelUpChoices.accept(playerData.gainExperience(crystal.getExperienceValue()));
                    } else if (crystal.getType() == DroppedCrystal.CrystalType.HEALTH) {
                        playerData.heal(HEALTH_PER_CRYSTAL);
                        sessionState.setActiveEffectName("Health Crystal");
                    }
                }
                crystalIterator.remove();
            }
        }
    }

    private void handleProjectileCollisions(PlayerSprite player, Player playerData, Runnable onPlayerDeath) {
        Iterator<Projectile> projectileIterator = world.getProjectiles().iterator();
        while (projectileIterator.hasNext()) {
            Projectile projectile = projectileIterator.next();
            if (projectile.isEnemyOwned()) {
                handleEnemyProjectileCollision(projectile, player, playerData, onPlayerDeath);
                continue;
            }

            for (Enemy enemy : world.getEnemies()) {
                if (projectile.hasImpacted() || !projectile.canDamage()) {
                    break;
                }
                if (!enemy.isDead() && projectile.intersects(enemy.getBoundingRectangle())) {
                    enemy.takeDamage(projectile.getDamage());
                    if (enemy.isDead()) {
                        world.spawnCrystalDrop(enemy);
                    }
                    projectile.markImpact();
                    break;
                }
            }
        }
    }

    private void handleEnemyProjectileCollision(Projectile projectile, PlayerSprite player, Player playerData,
                                                Runnable onPlayerDeath) {
        if (projectile.hasImpacted()) {
            return;
        }

        if (projectile.intersects(player.getBoundingRectangle())) {
            if (playerData != null) {
                playerData.takeDamage(projectile.getDamage());
                player.triggerDamageFlash();
                if (playerData.getHealth() <= 0) {
                    onPlayerDeath.run();
                }
            }
            projectile.markImpact();
        }
    }

    private Projectile createProjectile(int startX, int startY, int targetX, int targetY,
                                        double speed, int damage, boolean enemyOwned,
                                        String frameDirectory, double renderScale,
                                        WeaponType weaponType, Projectile.MotionMode motionMode) {
        double deltaX = targetX - startX;
        double deltaY = targetY - startY;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (distance == 0) {
            distance = 1;
        }

        double velocityX = (deltaX / distance) * speed;
        double velocityY = (deltaY / distance) * speed;
        double hitboxLengthScale = enemyOwned ? 0.38 : 0.42;
        double hitboxThicknessScale = enemyOwned ? 0.18 : 0.16;
        double rotationRadians = Math.atan2(deltaY, deltaX);
        return Projectile.create(startX, startY, velocityX, velocityY, damage, enemyOwned,
            frameDirectory, renderScale, true, rotationRadians, hitboxLengthScale, hitboxThicknessScale,
            weaponType, motionMode);
    }

    private double getDistance(int startX, int startY, int endX, int endY) {
        int deltaX = endX - startX;
        int deltaY = endY - startY;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
}

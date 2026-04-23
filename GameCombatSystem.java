import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.IntConsumer;

public class GameCombatSystem {
    private static final int HEALTH_PER_CRYSTAL = 20;
    private static final long GHOST_CONTACT_COOLDOWN_MS = 500L;
    private static final long BOSS_RANGED_ATTACK_COOLDOWN_MS = 1000L;
    private static final double BOSS_RANGED_PROJECTILE_SCALE = 0.25;
    private static final double BOSS_RANGED_SPAWN_OFFSET = 100.0;

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

        world.getEnemySpawner().update(deltaTimeMs, player, world.getEnemies(), world.getCurrentLevel());

        ArrayList<Enemy> removedEnemies = new ArrayList<Enemy>();
        Iterator<Enemy> enemyIterator = world.getEnemies().iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            if (enemy.shouldRemove()) {
                if (enemy.consumeDefeatReward()) {
                    world.spawnCrystalDrop(enemy);
                }
                removedEnemies.add(enemy);
                enemyIterator.remove();
                continue;
            }

            enemy.updateDamageFlash(deltaTimeMs);
            enemy.updateAttackCooldown(deltaTimeMs);

            if (enemy instanceof BatEnemy) {
                double distanceToPlayer = getDistance(enemy.getCenterX(), enemy.getCenterY(), player.getCenterX(),
                        player.getCenterY());
                if (enemy.isAlive()) {
                    updateBatEnemy(enemy, player, deltaTimeMs, distanceToPlayer);
                }
            } else if (enemy instanceof BossEnemy) {
                BossEnemy boss = (BossEnemy) enemy;
                boss.updateRangedAttackCooldown(deltaTimeMs);
                boss.updateBehavior(player, deltaTimeMs);
                updateBossRangedAttack(boss, player);
            } else if (enemy instanceof SkeletonEnemy) {
                ((SkeletonEnemy) enemy).updateBehavior(player, deltaTimeMs);
            } else if (enemy instanceof GhostEnemy) {
                ((GhostEnemy) enemy).rememberPlayerPosition(player);
                ((GhostEnemy) enemy).updateBehavior(player, deltaTimeMs, world.getWorldWidth(), world.getWorldHeight());
            } else if (enemy.isAlive()) {
                enemy.moveToward(player.getCenterX(), player.getCenterY(), deltaTimeMs);
            }

            enemy.update(deltaTimeMs);
        }

        for (Enemy removedEnemy : removedEnemies) {
            removedEnemy.onRemoved(world);
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

    private void updateBossRangedAttack(BossEnemy boss, PlayerSprite player) {
        if (!boss.isAlive() || !boss.supportsRangedAttack() || boss.isBusyWithMeleeAttack()
                || !boss.canUseRangedAttack()) {
            return;
        }

        int startX = boss.getCenterX();
        int startY = boss.getCenterY();
        int targetX = player.getCenterX();
        int targetY = player.getCenterY();
        double deltaX = targetX - startX;
        double deltaY = targetY - startY;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (distance > 0.001) {
            startX += (int) Math.round((deltaX / distance) * BOSS_RANGED_SPAWN_OFFSET);
            startY += (int) Math.round((deltaY / distance) * BOSS_RANGED_SPAWN_OFFSET);
        }

        world.getProjectiles().add(createProjectile(
                startX,
                startY,
                targetX,
                targetY,
                batBulletSpeed,
                boss.getContactDamage(),
                true,
                "images/spells/waterArrow",
                BOSS_RANGED_PROJECTILE_SCALE,
                WeaponType.FIRE_ARROW,
                Projectile.MotionMode.STRAIGHT));
        boss.setRangedAttackCooldown(BOSS_RANGED_ATTACK_COOLDOWN_MS);
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
        handleBossContactCollisions(player, playerData, playerBounds, onPlayerDeath);
        handleSkeletonDashCollisions(player, playerData, playerBounds, onPlayerDeath);
        handleGhostContactCollisions(player, playerData, playerBounds, onPlayerDeath);
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
                queueLevelUpChoices
                        .accept(playerData != null ? playerData.gainExperience(experiencePerCollectible) : 0);
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
                soundManager.playClip("crystal-sound", false);
                if (playerData != null) {
                    if (crystal.getType() == DroppedCrystal.CrystalType.EXPERIENCE) {
                        queueLevelUpChoices.accept(playerData.gainExperience(crystal.getExperienceValue()));
                    } else if (crystal.getType() == DroppedCrystal.CrystalType.HEALTH) {
                        playerData.heal(HEALTH_PER_CRYSTAL);
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
                if (enemy.canTakeDamage() && projectile.intersects(enemy.getBoundingRectangle())) {
                    enemy.takeDamage(projectile.getDamage());
                    soundManager.playClip("spell-attack-noise", false);
                    soundManager.playClip("enemy-hurt", false);
                    projectile.markImpact();
                    break;
                }
            }
        }
    }

    private void handleSkeletonDashCollisions(PlayerSprite player, Player playerData,
            Rectangle2D.Double playerBounds, Runnable onPlayerDeath) {
        if (playerData == null) {
            return;
        }

        for (Enemy enemy : world.getEnemies()) {
            if (!(enemy instanceof SkeletonEnemy)) {
                continue;
            }

            SkeletonEnemy skeleton = (SkeletonEnemy) enemy;
            if (!skeleton.canDamagePlayerOnDash()) {
                continue;
            }

            if (PixelCollision.intersects(playerBounds, player.getCurrentBufferedImage(),
                    skeleton.getBoundingRectangle(), skeleton.getCurrentBufferedImage())) {
                playerData.takeDamage(skeleton.getContactDamage());
                soundManager.playClip("player-hurt", false);
                player.triggerDamageFlash();
                skeleton.markDashHitApplied();
                if (playerData.getHealth() <= 0) {
                    soundManager.playClip("player-dies", false);
                    onPlayerDeath.run();
                }
            }
        }
    }

    private void handleBossContactCollisions(PlayerSprite player, Player playerData,
            Rectangle2D.Double playerBounds, Runnable onPlayerDeath) {
        if (playerData == null) {
            return;
        }

        for (Enemy enemy : world.getEnemies()) {
            if (!(enemy instanceof BossEnemy) || !enemy.isTargetable()) {
                continue;
            }

            BossEnemy boss = (BossEnemy) enemy;
            if (!boss.hasAttackDamagePending() || !boss.isOnAttackImpactFrame()) {
                continue;
            }

            if (PixelCollision.intersects(playerBounds, player.getCurrentBufferedImage(),
                    enemy.getBoundingRectangle(), enemy.getCurrentBufferedImage())) {
                playerData.takeDamage(enemy.getContactDamage());
                soundManager.playClip("player-hurt", false);
                player.triggerDamageFlash();
                boss.consumeAttackDamage();
                if (playerData.getHealth() <= 0) {
                    soundManager.playClip("player-dies", false);
                    onPlayerDeath.run();
                }
            }
        }
    }

    private void handleGhostContactCollisions(PlayerSprite player, Player playerData,
            Rectangle2D.Double playerBounds, Runnable onPlayerDeath) {
        if (playerData == null) {
            return;
        }

        for (Enemy enemy : world.getEnemies()) {
            if (!(enemy instanceof GhostEnemy) || !enemy.canAttack() || !enemy.isTargetable()) {
                continue;
            }

            if (PixelCollision.intersects(playerBounds, player.getCurrentBufferedImage(),
                    enemy.getBoundingRectangle(), enemy.getCurrentBufferedImage())) {
                playerData.takeDamage(enemy.getContactDamage());
                soundManager.playClip("player-hurt", false);
                player.triggerDamageFlash();
                enemy.setAttackCooldown(GHOST_CONTACT_COOLDOWN_MS);
                if (playerData.getHealth() <= 0) {
                    soundManager.playClip("player-dies", false);
                    onPlayerDeath.run();
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
                soundManager.playClip("player-hurt", false);
                player.triggerDamageFlash();
                if (playerData.getHealth() <= 0) {
                    soundManager.playClip("player-dies", false);
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

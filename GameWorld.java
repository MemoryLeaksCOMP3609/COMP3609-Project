import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class GameWorld {
    private static final int COLLECTIBLE_SIZE = 40;
    private static final int COLLECTIBLE_MIN_DISTANCE = 200;
    private static final double DROP_NOTHING_CHANCE = 0.30;
    private static final double DROP_EXPERIENCE_CHANCE = 0.60;
    private static final double CRYSTAL_SCALE = 0.10;

    private final int worldWidth;
    private final int worldHeight;
    private final WorldGenerator worldGenerator;
    private final BufferedImage backgroundImage;
    private final EnemySpawner enemySpawner;
    private final Random random;
    private final BufferedImage experienceCrystalTierOneImage;
    private final BufferedImage experienceCrystalTierTwoImage;
    private final BufferedImage experienceCrystalTierThreeImage;
    private final BufferedImage experienceCrystalTierFourImage;
    private final BufferedImage healthCrystalImage;

    private Player playerData;
    private PlayerSprite player;
    private ArrowSprite arrowSprite;
    private ArrayList<AnimatedSprite> animatedSprites;
    private ArrayList<SolidObject> solidObjects;
    private ArrayList<Collectible> collectibles;
    private ArrayList<Enemy> enemies;
    private ArrayList<Projectile> projectiles;
    private ArrayList<DroppedCrystal> droppedCrystals;
    private int cameraX;
    private int cameraY;

    public GameWorld(int worldWidth, int worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.worldGenerator = new WorldGenerator(worldWidth, worldHeight);
        this.worldGenerator.loadImages();
        this.backgroundImage = ImageManager.loadBufferedImage("images/worldBackgroundSmall.png");
        this.enemySpawner = new EnemySpawner(worldWidth, worldHeight);
        this.random = new Random();
        this.experienceCrystalTierOneImage = ImageManager.scaleImageByFactor(
            ImageManager.loadBufferedImage("images/objects/crystals/normal/crystalNormal8.png"), CRYSTAL_SCALE);
        this.experienceCrystalTierTwoImage = ImageManager.scaleImageByFactor(
            ImageManager.loadBufferedImage("images/objects/crystals/normal/crystalNormal16.png"), CRYSTAL_SCALE);
        this.experienceCrystalTierThreeImage = ImageManager.scaleImageByFactor(
            ImageManager.loadBufferedImage("images/objects/crystals/normal/crystalNormal19.png"), CRYSTAL_SCALE);
        this.experienceCrystalTierFourImage = ImageManager.scaleImageByFactor(
            ImageManager.loadBufferedImage("images/objects/crystals/normal/crystalNormal9.png"), CRYSTAL_SCALE);
        this.healthCrystalImage = ImageManager.scaleImageByFactor(
            ImageManager.loadBufferedImage("images/objects/crystals/normal/crystalNormal14.png"), CRYSTAL_SCALE);
        this.animatedSprites = new ArrayList<AnimatedSprite>();
        this.solidObjects = new ArrayList<SolidObject>();
        this.collectibles = new ArrayList<Collectible>();
        this.enemies = new ArrayList<Enemy>();
        this.projectiles = new ArrayList<Projectile>();
        this.droppedCrystals = new ArrayList<DroppedCrystal>();
        this.cameraX = 0;
        this.cameraY = 0;
    }

    public void initializeEntities(javax.swing.JPanel panel, int winCollectibles) {
        int playerStartX = worldWidth / 2 - 25;
        int playerStartY = worldHeight / 2 - 25;
        playerData = new Player();
        player = new PlayerSprite(panel, playerData, playerStartX, playerStartY, worldWidth, worldHeight);
        solidObjects = worldGenerator.createSolidObjects(25, playerStartX, playerStartY, 250);
        collectibles = worldGenerator.createCollectibles(solidObjects, winCollectibles, COLLECTIBLE_SIZE, COLLECTIBLE_MIN_DISTANCE);
        animatedSprites = worldGenerator.createAnimatedSprites(panel);
        arrowSprite = new ArrowSprite();
        enemies = new ArrayList<Enemy>();
        projectiles = new ArrayList<Projectile>();
        droppedCrystals = new ArrayList<DroppedCrystal>();
        enemySpawner.reset();
        cameraX = 0;
        cameraY = 0;
    }

    public void respawnCollectedCollectible(Collectible collectedCollectible) {
        collectibles.remove(collectedCollectible);
        collectibles.add(worldGenerator.createCollectible(solidObjects, collectibles, COLLECTIBLE_SIZE, COLLECTIBLE_MIN_DISTANCE));
    }

    public void updateCamera(int panelWidth, int panelHeight) {
        if (player == null) {
            return;
        }

        cameraX = player.getWorldX() - panelWidth / 2 + player.getWidth() / 2;
        cameraY = player.getWorldY() - panelHeight / 2 + player.getHeight() / 2;
        cameraX = Math.max(0, Math.min(cameraX, worldWidth - panelWidth));
        cameraY = Math.max(0, Math.min(cameraY, worldHeight - panelHeight));
    }

    public void updateScreenPositions() {
        if (player != null) {
            player.updateScreenPosition(cameraX, cameraY);
        }

        for (AnimatedSprite sprite : animatedSprites) {
            sprite.updateScreenPosition(cameraX, cameraY);
        }

        for (Collectible collectible : collectibles) {
            collectible.updateScreenPosition(cameraX, cameraY);
        }

        for (Enemy enemy : enemies) {
            enemy.updateScreenPosition(cameraX, cameraY);
        }

        for (DroppedCrystal crystal : droppedCrystals) {
            crystal.updateScreenPosition(cameraX, cameraY);
        }

        if (arrowSprite != null && player != null) {
            arrowSprite.update(player.getScreenX(), player.getScreenY(), collectibles);
        }
    }

    public void updateWorldAnimations() {
        for (AnimatedSprite sprite : animatedSprites) {
            sprite.update();
        }

        for (Collectible collectible : collectibles) {
            collectible.update();
        }
    }

    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    public PlayerSprite getPlayer() {
        return player;
    }

    public Player getPlayerData() {
        return playerData;
    }

    public ArrowSprite getArrowSprite() {
        return arrowSprite;
    }

    public ArrayList<AnimatedSprite> getAnimatedSprites() {
        return animatedSprites;
    }

    public ArrayList<SolidObject> getSolidObjects() {
        return solidObjects;
    }

    public ArrayList<Collectible> getCollectibles() {
        return collectibles;
    }

    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }

    public ArrayList<Projectile> getProjectiles() {
        return projectiles;
    }

    public ArrayList<DroppedCrystal> getDroppedCrystals() {
        return droppedCrystals;
    }

    public int getCameraX() {
        return cameraX;
    }

    public int getCameraY() {
        return cameraY;
    }

    public int getWorldWidth() {
        return worldWidth;
    }

    public int getWorldHeight() {
        return worldHeight;
    }

    public EnemySpawner getEnemySpawner() {
        return enemySpawner;
    }

    public void spawnCrystalDrop(Enemy enemy) {
        double roll = random.nextDouble();
        if (roll < DROP_NOTHING_CHANCE) {
            return;
        }

        if (roll < DROP_NOTHING_CHANCE + DROP_EXPERIENCE_CHANCE) {
            DroppedCrystal.ExperienceTier tier = rollExperienceTier(enemy);
            BufferedImage image = getExperienceCrystalImage(tier);
            if (image == null) {
                return;
            }
            int dropX = enemy.getCenterX() - image.getWidth() / 2;
            int dropY = enemy.getCenterY() - image.getHeight() / 2;
            droppedCrystals.add(new DroppedCrystal(dropX, dropY, image, DroppedCrystal.CrystalType.EXPERIENCE, tier));
        } else {
            if (healthCrystalImage == null) {
                return;
            }
            int dropX = enemy.getCenterX() - healthCrystalImage.getWidth() / 2;
            int dropY = enemy.getCenterY() - healthCrystalImage.getHeight() / 2;
            droppedCrystals.add(new DroppedCrystal(dropX, dropY, healthCrystalImage, DroppedCrystal.CrystalType.HEALTH, null));
        }
    }

    private DroppedCrystal.ExperienceTier rollExperienceTier(Enemy enemy) {
        double tierRoll = random.nextDouble();

        if (enemy instanceof BatEnemy) {
            return DroppedCrystal.ExperienceTier.TIER_1;
        }
        if (enemy instanceof NormalSkeletonEnemy) {
            return tierRoll < 0.70 ? DroppedCrystal.ExperienceTier.TIER_1 : DroppedCrystal.ExperienceTier.TIER_2;
        }
        if (enemy instanceof NormalGhostEnemy) {
            return DroppedCrystal.ExperienceTier.TIER_2;
        }
        if (enemy instanceof DarkGhostEnemy) {
            if (tierRoll < 0.50) {
                return DroppedCrystal.ExperienceTier.TIER_2;
            }
            if (tierRoll < 0.95) {
                return DroppedCrystal.ExperienceTier.TIER_3;
            }
            return DroppedCrystal.ExperienceTier.TIER_4;
        }
        if (enemy instanceof ToxicSkeletonEnemy) {
            return DroppedCrystal.ExperienceTier.TIER_3;
        }
        if (enemy instanceof FrostGhostEnemy) {
            return tierRoll < 0.70 ? DroppedCrystal.ExperienceTier.TIER_3 : DroppedCrystal.ExperienceTier.TIER_4;
        }

        return DroppedCrystal.ExperienceTier.TIER_1;
    }

    private BufferedImage getExperienceCrystalImage(DroppedCrystal.ExperienceTier tier) {
        switch (tier) {
            case TIER_1:
                return experienceCrystalTierOneImage;
            case TIER_2:
                return experienceCrystalTierTwoImage;
            case TIER_3:
                return experienceCrystalTierThreeImage;
            case TIER_4:
                return experienceCrystalTierFourImage;
            default:
                return experienceCrystalTierOneImage;
        }
    }
}

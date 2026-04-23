import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GameWorld {
    private static final int COLLECTIBLE_SIZE = 40;
    private static final int COLLECTIBLE_MIN_DISTANCE = 200;
    private static final double DROP_NOTHING_CHANCE = 0.30;
    private static final double DROP_EXPERIENCE_CHANCE = 0.60;
    private static final double CRYSTAL_SCALE = 0.10;

    private static final int BOSS_SPAWN_OFFSET = 800;

    private static final int LEVEL_BOSS_GRASS = 10;
    private static final int LEVEL_BOSS_DESERT = 15;
    private static final int LEVEL_BOSS_ICE = 20;

    public enum OverlayState {
        NONE,
        WARNING_FADE_IN,
        WARNING_HOLD,
        WARNING_FADE_OUT,
        DISCLAIMER_FADE_IN,
        DISCLAIMER_HOLD,
        DISCLAIMER_FADE_OUT
    }

    private OverlayState overlayState = OverlayState.NONE;
    private float overlayAlpha = 0f; // 0 = transparent, 1 = opaque
    private long overlayHoldMs = 0;
    private BufferedImage overlayImage = null; // warning or disclaimer image

    private static final float FADE_STEP_PER_MS = 1.0f / 500f; // 500 ms fade
    private static final long HOLD_DURATION_MS = 1500;

    private BufferedImage warningImage;
    private BufferedImage disclaimer1Image;
    private BufferedImage disclaimer2Image;
    private BufferedImage disclaimer3Image;

    private boolean bossSpawnPending = false;

    private int pendingBossLevel = 0;

    private boolean grassBossTriggered = false;
    private boolean desertBossTriggered = false;
    private boolean iceBossTriggered = false;

    private boolean grassBossDead = false;
    private boolean desertBossDead = false;
    private boolean iceBossDead = false;

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
    private int viewportWidth;
    private int viewportHeight;

    private TileMap tileMap;
    private TileMapManager tileMapManager;
    private LevelPortal levelPortal;
    private int currentLevel = 1;

    public GameWorld(int worldWidth, int worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.worldGenerator = new WorldGenerator(worldWidth, worldHeight);
        this.worldGenerator.loadImages();
        this.backgroundImage = ImageManager.loadBufferedImage("images/worldBackgroundSmall.png");
        this.enemySpawner = new EnemySpawner(worldWidth, worldHeight);
        this.random = new Random();

        this.experienceCrystalTierOneImage = scaled("images/objects/crystals/normal/crystalNormal8.png");
        this.experienceCrystalTierTwoImage = scaled("images/objects/crystals/normal/crystalNormal16.png");
        this.experienceCrystalTierThreeImage = scaled("images/objects/crystals/normal/crystalNormal19.png");
        this.experienceCrystalTierFourImage = scaled("images/objects/crystals/normal/crystalNormal9.png");
        this.healthCrystalImage = scaled("images/objects/crystals/normal/crystalNormal14.png");

        this.animatedSprites = new ArrayList<>();
        this.solidObjects = new ArrayList<>();
        this.collectibles = new ArrayList<>();
        this.enemies = new ArrayList<>();
        this.projectiles = new ArrayList<>();
        this.droppedCrystals = new ArrayList<>();

        warningImage = ImageManager.loadBufferedImage("images/warning.png");
        disclaimer1Image = ImageManager.loadBufferedImage("images/disclaimer1.png");
        disclaimer2Image = ImageManager.loadBufferedImage("images/disclaimer2.png");
        disclaimer3Image = ImageManager.loadBufferedImage("images/disclaimer3.png");

        this.tileMapManager = new TileMapManager();
        try {
            this.tileMap = tileMapManager.loadMap("maps/map1.txt", "grass");
            this.tileMap.setCurrentLevel(1);
            System.out.println("TileMap loaded successfully");
        } catch (Exception e) {
            System.out.println("Failed to load tilemap: " + e.getMessage());
            this.tileMap = null;
        }

        this.enemySpawner.setOnBossDefeatedCallback(this::notifyBossDefeated);
    }

    private BufferedImage scaled(String path) {
        return ImageManager.scaleImageByFactor(ImageManager.loadBufferedImage(path), CRYSTAL_SCALE);
    }

    public TileMap getTileMap() {
        return tileMap;
    }

    public void loadLevel(int levelNumber) {
        String mapFile;
        String terrain;
        int spawnTileX;
        int spawnTileY;

        this.currentLevel = levelNumber;

        switch (levelNumber) {
            case 2:
                mapFile = "maps/map2.txt";
                terrain = "desert";
                spawnTileX = 7;
                spawnTileY = 16;
                levelPortal = buildPortal(28 * 100, 16 * 100, 2);
                break;
            case 3:
                mapFile = "maps/map3.txt";
                terrain = "ice";
                spawnTileX = 0;
                spawnTileY = 17;
                levelPortal = buildPortal(28 * 100, 7 * 100, 3);
                break;
            default:
                mapFile = "maps/map1.txt";
                terrain = "grass";
                spawnTileX = 7;
                spawnTileY = 19;
                levelPortal = buildPortal(28 * 100, 19 * 100, 1);
                break;
        }

        try {
            tileMap = tileMapManager.loadMap(mapFile, terrain);
            tileMap.setCurrentLevel(levelNumber);

            ObjectMapLoader objectLoader = new ObjectMapLoader();
            switch (levelNumber) {
                case 2:
                    solidObjects = objectLoader.loadObjects("maps/desert_objects.txt", "desert");
                    break;
                case 3:
                    solidObjects = objectLoader.loadObjects("maps/ice_objects.txt", "ice");
                    break;
                default:
                    solidObjects = objectLoader.loadObjects("maps/grass_objects.txt", "grass");
                    break;
            }

            if (player != null) {
                player.setWorldX(spawnTileX * 100);
                player.setWorldY(spawnTileY * 100);
            }

            resetBossTriggerForLevel(levelNumber);

            System.out.println("Loaded level " + levelNumber + " (" + terrain + ")");
        } catch (Exception e) {
            System.out.println("Failed to load level " + levelNumber + ": " + e.getMessage());
        }
    }

    public int getNextLevel() {
        return (currentLevel >= 3) ? 1 : currentLevel + 1;
    }

    private LevelPortal buildPortal(int x, int y, int portalLevel) {
        LevelPortal p = new LevelPortal(x, y);
        p.setPortalLevel(portalLevel);

        switch (portalLevel) {
            case 1:
                if (grassBossDead)
                    p.notifyBossDead();
                break;
            case 2:
                if (desertBossDead)
                    p.notifyBossDead();
                break;
            case 3:
                if (iceBossDead)
                    p.notifyBossDead();
                break;
        }
        return p;
    }

    private void resetBossTriggerForLevel(int level) {
        switch (level) {
            case 1:
                if (!grassBossDead)
                    grassBossTriggered = false;
                break;
            case 2:
                if (!desertBossDead)
                    desertBossTriggered = false;
                break;
            case 3:
                if (!iceBossDead)
                    iceBossTriggered = false;
                break;
        }
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void initializeEntities(javax.swing.JPanel panel, int winCollectibles) {
        int playerStartX = 7 * 100;
        int playerStartY = 19 * 100;

        playerData = new Player();
        player = new PlayerSprite(panel, playerData, playerStartX, playerStartY,
                worldWidth, worldHeight);

        String terrain = terrainForLevel(currentLevel);

        ObjectMapLoader objectLoader = new ObjectMapLoader();
        solidObjects = objectLoader.loadObjects("maps/grass_objects.txt", "grass");

        collectibles = worldGenerator.createCollectibles(solidObjects, winCollectibles,
                COLLECTIBLE_SIZE, COLLECTIBLE_MIN_DISTANCE);
        animatedSprites = worldGenerator.createAnimatedSprites(panel);
        arrowSprite = new ArrowSprite();
        enemies = new ArrayList<>();
        projectiles = new ArrayList<>();
        droppedCrystals = new ArrayList<>();
        enemySpawner.reset();
        cameraX = 0;
        cameraY = 0;

        levelPortal = buildPortal(28 * 100, 19 * 100, 1);
    }

    public LevelPortal getLevelPortal() {
        return levelPortal;
    }

    public void updateBossSystem(long deltaTimeMs, int playerLevel) {
        checkBossTrigger(playerLevel);
        tickOverlay(deltaTimeMs);
    }

    private void checkBossTrigger(int playerLevel) {
        if (overlayState != OverlayState.NONE || bossSpawnPending)
            return;

        if (currentLevel == 1 && !grassBossTriggered && playerLevel >= LEVEL_BOSS_GRASS) {
            grassBossTriggered = true;
            pendingBossLevel = LEVEL_BOSS_GRASS;
            startWarningOverlay();
        } else if (currentLevel == 2 && !desertBossTriggered && playerLevel >= LEVEL_BOSS_DESERT) {
            desertBossTriggered = true;
            pendingBossLevel = LEVEL_BOSS_DESERT;
            startWarningOverlay();
        } else if (currentLevel == 3 && !iceBossTriggered && playerLevel >= LEVEL_BOSS_ICE) {
            iceBossTriggered = true;
            pendingBossLevel = LEVEL_BOSS_ICE;
            startWarningOverlay();
        }
    }

    private void startWarningOverlay() {
        overlayImage = warningImage;
        overlayAlpha = 0f;
        overlayState = OverlayState.WARNING_FADE_IN;
        bossSpawnPending = true;
        System.out.println("Boss warning triggered for level " + pendingBossLevel);
    }

    private void tickOverlay(long deltaTimeMs) {
        if (overlayState == OverlayState.NONE)
            return;

        float fadeStep = FADE_STEP_PER_MS * deltaTimeMs;

        switch (overlayState) {

            case WARNING_FADE_IN:
                overlayAlpha = Math.min(1f, overlayAlpha + fadeStep);
                if (overlayAlpha >= 1f) {
                    overlayAlpha = 1f;
                    overlayHoldMs = HOLD_DURATION_MS;
                    overlayState = OverlayState.WARNING_HOLD;
                }
                break;

            case WARNING_HOLD:
                overlayHoldMs -= deltaTimeMs;
                if (overlayHoldMs <= 0) {
                    overlayState = OverlayState.WARNING_FADE_OUT;
                }
                break;

            case WARNING_FADE_OUT:
                overlayAlpha = Math.max(0f, overlayAlpha - fadeStep);
                if (overlayAlpha <= 0f) {
                    overlayAlpha = 0f;
                    overlayState = OverlayState.NONE;
                    overlayImage = null;
                    bossSpawnPending = false;
                    spawnBossForLevel(pendingBossLevel);
                }
                break;

            case DISCLAIMER_FADE_IN:
                overlayAlpha = Math.min(1f, overlayAlpha + fadeStep);
                if (overlayAlpha >= 1f) {
                    overlayAlpha = 1f;
                    overlayHoldMs = HOLD_DURATION_MS;
                    overlayState = OverlayState.DISCLAIMER_HOLD;
                }
                break;

            case DISCLAIMER_HOLD:
                overlayHoldMs -= deltaTimeMs;
                if (overlayHoldMs <= 0) {
                    overlayState = OverlayState.DISCLAIMER_FADE_OUT;
                }
                break;

            case DISCLAIMER_FADE_OUT:
                overlayAlpha = Math.max(0f, overlayAlpha - fadeStep);
                if (overlayAlpha <= 0f) {
                    overlayAlpha = 0f;
                    overlayState = OverlayState.NONE;
                    overlayImage = null;
                    activatePortalForCurrentLevel();
                }
                break;

            default:
                break;
        }
    }

    private void spawnBossForLevel(int triggerLevel) {
        if (player == null)
            return;

        // Clear all existing enemies before spawning the boss
        enemies.clear();

        int spawnX = clamp(player.getWorldX() - BOSS_SPAWN_OFFSET, 0, worldWidth - 300);
        int spawnY = clamp(player.getWorldY() - BOSS_SPAWN_OFFSET, 0, worldHeight - 300);

        Enemy boss;
        switch (triggerLevel) {
            case LEVEL_BOSS_GRASS:
                boss = new BossPhaseOneEnemy(spawnX, spawnY);
                break;
            case LEVEL_BOSS_DESERT:
                boss = new BossPhaseTwoEnemy(spawnX, spawnY);
                break;
            case LEVEL_BOSS_ICE:
                boss = new BossPhaseThreeEnemy(spawnX, spawnY);
                break;
            default:
                System.out.println("Unknown boss trigger level: " + triggerLevel);
                return;
        }

        enemies.add(boss);
        System.out.println("Boss spawned at (" + spawnX + ", " + spawnY
                + ") for trigger level " + triggerLevel);
    }

    private void notifyBossDefeated() {
        switch (currentLevel) {
            case 1:
                if (!grassBossDead) {
                    grassBossDead = true;
                    startDisclaimerOverlay(1);
                }
                break;
            case 2:
                if (!desertBossDead) {
                    desertBossDead = true;
                    startDisclaimerOverlay(2);
                }
                break;
            case 3:
                if (!iceBossDead) {
                    iceBossDead = true;
                    startDisclaimerOverlay(3);
                }
                break;
        }
    }

    private void startDisclaimerOverlay(int forLevel) {
        overlayImage = (forLevel == 1) ? disclaimer1Image : (forLevel == 2) ? disclaimer2Image : disclaimer3Image;
        overlayAlpha = 0f;
        overlayState = OverlayState.DISCLAIMER_FADE_IN;
        System.out.println("Disclaimer overlay started for level " + forLevel);
    }

    // Called once the disclaimer finishes fading out – marks portal as open.
    private void activatePortalForCurrentLevel() {
        if (levelPortal == null)
            return;
        levelPortal.notifyBossDead();
        System.out.println("Portal activated for level " + currentLevel);
    }

    public boolean isOverlayActive() {
        return overlayState != OverlayState.NONE && overlayImage != null;
    }

    public BufferedImage getOverlayImage() {
        return overlayImage;
    }

    // Returns 0-255 alpha for the current overlay.
    public int getOverlayAlpha() {
        return Math.round(overlayAlpha * 255f);
    }

    public OverlayState getOverlayState() {
        return overlayState;
    }

    public void respawnCollectedCollectible(Collectible collectedCollectible) {
        collectibles.remove(collectedCollectible);
        collectibles.add(worldGenerator.createCollectible(solidObjects, collectibles,
                COLLECTIBLE_SIZE, COLLECTIBLE_MIN_DISTANCE));
    }

    public void updateCamera(int panelWidth, int panelHeight) {
        if (player == null)
            return;

        viewportWidth = Math.max(1, panelWidth);
        viewportHeight = Math.max(1, panelHeight);
        cameraX = player.getWorldX() - panelWidth / 2 + player.getWidth() / 2;
        cameraY = player.getWorldY() - panelHeight / 2 + player.getHeight() / 2;
        cameraX = Math.max(0, Math.min(cameraX, worldWidth - panelWidth));
        cameraY = Math.max(0, Math.min(cameraY, worldHeight - panelHeight));
    }

    public void updateScreenPositions() {
        if (player != null)
            player.updateScreenPosition(cameraX, cameraY);

        for (AnimatedSprite sprite : animatedSprites)
            sprite.updateScreenPosition(cameraX, cameraY);
        for (Collectible c : collectibles)
            c.updateScreenPosition(cameraX, cameraY);
        for (Enemy e : enemies)
            e.updateScreenPosition(cameraX, cameraY);
        for (DroppedCrystal d : droppedCrystals)
            d.updateScreenPosition(cameraX, cameraY);

        if (arrowSprite != null && player != null) {
            int playerCenterX = player.getScreenX() + player.getWidth() / 2;
            int playerCenterY = player.getScreenY() + player.getHeight() / 2;
            arrowSprite.update(playerCenterX, playerCenterY,
                    viewportWidth, viewportHeight, collectibles);
        }
    }

    public void updateWorldAnimations() {
        for (AnimatedSprite sprite : animatedSprites)
            sprite.update();
        for (Collectible c : collectibles)
            c.update();
    }

    public boolean activateTestEnemySpawn(TestEnemySpawnType spawnType) {
        if (spawnType == null)
            return false;
        enemySpawner.setActiveSpawnType(spawnType);
        purgeEnemiesExcept(spawnType);
        clearEnemyProjectiles();
        return true;
    }

    public boolean spawnTestBoss(TestBossSpawnType bossType) {
        if (player == null || bossType == null)
            return false;
        purgeAllEnemies();
        clearEnemyProjectiles();
        int spawnX = clamp(player.getWorldX() + 220, 0, worldWidth - 250);
        int spawnY = clamp(player.getWorldY(), 0, worldHeight - 250);
        enemies.add(bossType.createBoss(spawnX, spawnY));
        return true;
    }

    private void purgeAllEnemies() {
        enemies.clear();
    }

    private void purgeEnemiesExcept(TestEnemySpawnType allowedType) {
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (allowedType == null || !allowedType.matches(e))
                it.remove();
        }
    }

    private void clearEnemyProjectiles() {
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            if (it.next().isEnemyOwned())
                it.remove();
        }
    }

    public void spawnCrystalDrop(Enemy enemy) {
        if (enemy instanceof BossEnemy)
            return;

        double roll = random.nextDouble();
        if (roll < DROP_NOTHING_CHANCE)
            return;

        if (roll < DROP_NOTHING_CHANCE + DROP_EXPERIENCE_CHANCE) {
            DroppedCrystal.ExperienceTier tier = rollExperienceTier(enemy);
            BufferedImage image = getExperienceCrystalImage(tier);
            if (image == null)
                return;
            int dropX = enemy.getCenterX() - image.getWidth() / 2;
            int dropY = enemy.getCenterY() - image.getHeight() / 2;
            droppedCrystals.add(new DroppedCrystal(dropX, dropY, image,
                    DroppedCrystal.CrystalType.EXPERIENCE, tier));
        } else {
            if (healthCrystalImage == null)
                return;
            int dropX = enemy.getCenterX() - healthCrystalImage.getWidth() / 2;
            int dropY = enemy.getCenterY() - healthCrystalImage.getHeight() / 2;
            droppedCrystals.add(new DroppedCrystal(dropX, dropY, healthCrystalImage,
                    DroppedCrystal.CrystalType.HEALTH, null));
        }
    }

    private DroppedCrystal.ExperienceTier rollExperienceTier(Enemy enemy) {
        double roll = random.nextDouble();
        if (enemy instanceof BatEnemy)
            return DroppedCrystal.ExperienceTier.TIER_1;
        if (enemy instanceof NormalSkeletonEnemy)
            return roll < 0.70 ? DroppedCrystal.ExperienceTier.TIER_1 : DroppedCrystal.ExperienceTier.TIER_2;
        if (enemy instanceof NormalGhostEnemy)
            return DroppedCrystal.ExperienceTier.TIER_2;
        if (enemy instanceof DarkGhostEnemy) {
            if (roll < 0.50)
                return DroppedCrystal.ExperienceTier.TIER_2;
            if (roll < 0.95)
                return DroppedCrystal.ExperienceTier.TIER_3;
            return DroppedCrystal.ExperienceTier.TIER_4;
        }
        if (enemy instanceof ToxicSkeletonEnemy)
            return DroppedCrystal.ExperienceTier.TIER_3;
        if (enemy instanceof FrostGhostEnemy)
            return roll < 0.70 ? DroppedCrystal.ExperienceTier.TIER_3 : DroppedCrystal.ExperienceTier.TIER_4;
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

    private String terrainForLevel(int level) {
        switch (level) {
            case 2:
                return "desert";
            case 3:
                return "ice";
            default:
                return "grass";
        }
    }

    public String getTerrainForCurrentLevel() {
        return terrainForLevel(currentLevel);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
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
}
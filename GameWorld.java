import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class GameWorld {
    private static final int COLLECTIBLE_SIZE = 40;
    private static final int COLLECTIBLE_MIN_DISTANCE = 200;

    private final int worldWidth;
    private final int worldHeight;
    private final WorldGenerator worldGenerator;
    private final BufferedImage backgroundImage;
    private final EnemySpawner enemySpawner;

    private Player playerData;
    private PlayerSprite player;
    private ArrowSprite arrowSprite;
    private ArrayList<AnimatedSprite> animatedSprites;
    private ArrayList<SolidObject> solidObjects;
    private ArrayList<Collectible> collectibles;
    private ArrayList<Enemy> enemies;
    private ArrayList<Projectile> projectiles;
    private int cameraX;
    private int cameraY;

    public GameWorld(int worldWidth, int worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.worldGenerator = new WorldGenerator(worldWidth, worldHeight);
        this.worldGenerator.loadImages();
        this.backgroundImage = ImageManager.loadBufferedImage("images/worldBackgroundSmall.png");
        this.enemySpawner = new EnemySpawner(worldWidth, worldHeight);
        this.animatedSprites = new ArrayList<AnimatedSprite>();
        this.solidObjects = new ArrayList<SolidObject>();
        this.collectibles = new ArrayList<Collectible>();
        this.enemies = new ArrayList<Enemy>();
        this.projectiles = new ArrayList<Projectile>();
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
            sprite.update();
        }

        for (Collectible collectible : collectibles) {
            collectible.updateScreenPosition(cameraX, cameraY);
            collectible.update();
        }

        for (Enemy enemy : enemies) {
            enemy.updateScreenPosition(cameraX, cameraY);
            enemy.update();
        }

        if (arrowSprite != null && player != null) {
            arrowSprite.update(player.getScreenX(), player.getScreenY(), collectibles);
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

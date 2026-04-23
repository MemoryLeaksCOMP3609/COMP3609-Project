import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

// Handles creation and placement of game entities in the world.

public class WorldGenerator {

    private Random random;
    private int worldWidth;
    private int worldHeight;

    private BufferedImage[] treeImages;
    private BufferedImage[] rockImages;

    public WorldGenerator(int worldWidth, int worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.random = new Random();
    }

    public void loadImages() {
        treeImages = ImageManager.loadTreeImages(150);
        rockImages = ImageManager.loadRockImages(50);

        int treesLoaded = 0;
        int rocksLoaded = 0;
        for (BufferedImage img : treeImages) {
            if (img != null)
                treesLoaded++;
        }
        for (BufferedImage img : rockImages) {
            if (img != null)
                rocksLoaded++;
        }
        System.out.println("Loaded " + treesLoaded + " tree images and " + rocksLoaded + " rock images");
    }

    public ArrayList<SolidObject> createSolidObjects(int numObjects,
            int playerStartX,
            int playerStartY,
            int safeZoneRadius) {
        ArrayList<SolidObject> solids = new ArrayList<SolidObject>();

        final int MAX_ATTEMPTS_PER_OBJECT = 1000;
        final int EDGE_MARGIN = 50;

        for (int i = 0; i < numObjects; i++) {
            int attempts = 0;
            boolean objectPlaced = false;

            while (attempts < MAX_ATTEMPTS_PER_OBJECT && !objectPlaced) {
                attempts++;

                boolean isTree = random.nextBoolean();
                BufferedImage selectedImage;

                if (isTree && treeImages != null && treeImages.length > 0) {
                    selectedImage = treeImages[random.nextInt(treeImages.length)];
                } else if (rockImages != null && rockImages.length > 0) {
                    selectedImage = rockImages[random.nextInt(rockImages.length)];
                } else {
                    break;
                }

                if (selectedImage == null)
                    break;

                int objX = EDGE_MARGIN + random.nextInt(Math.max(1, worldWidth - 100));
                int objY = EDGE_MARGIN + random.nextInt(Math.max(1, worldHeight - 100));

                double distToPlayer = Math.hypot(objX - playerStartX, objY - playerStartY);
                if (distToPlayer < safeZoneRadius)
                    continue;

                boolean overlaps = false;
                Rectangle2D.Double newBounds = new Rectangle2D.Double(
                        objX, objY, selectedImage.getWidth(), selectedImage.getHeight());

                for (SolidObject existing : solids) {
                    if (newBounds.intersects(existing.getBoundingRectangle())) {
                        overlaps = true;
                        break;
                    }
                }

                if (!overlaps) {
                    solids.add(new SolidObject(objX, objY, selectedImage));
                    objectPlaced = true;
                }
            }
        }

        System.out.println("Created " + solids.size() + " random solid objects (legacy)");
        return solids;
    }

    public ArrayList<Collectible> createCollectibles(ArrayList<SolidObject> solidObjects,
            int numCollectibles,
            int collectibleSize,
            int minDistanceFromSolid) {
        ArrayList<Collectible> collectibles = new ArrayList<Collectible>();
        for (int i = 0; i < numCollectibles; i++) {
            collectibles.add(createCollectible(solidObjects, collectibles,
                    collectibleSize, minDistanceFromSolid));
        }
        System.out.println("Created " + collectibles.size() + " animated coin collectibles");
        return collectibles;
    }

    public Collectible createCollectible(ArrayList<SolidObject> solidObjects,
            ArrayList<Collectible> existingCollectibles,
            int collectibleSize,
            int minDistanceFromSolid) {
        int[] position = findCollectibleSpawnPosition(solidObjects, existingCollectibles,
                collectibleSize, minDistanceFromSolid);
        return createCoinCollectible(position[0], position[1], collectibleSize);
    }

    private int[] findCollectibleSpawnPosition(ArrayList<SolidObject> solidObjects,
            ArrayList<Collectible> existingCollectibles,
            int collectibleSize,
            int minDistanceFromSolid) {
        final int EDGE_MARGIN = 50;
        final int MAX_ATTEMPTS = 1000;

        int maxX = EDGE_MARGIN + Math.max(0, worldWidth - 2 * EDGE_MARGIN - collectibleSize);
        int maxY = EDGE_MARGIN + Math.max(0, worldHeight - 2 * EDGE_MARGIN - collectibleSize);

        for (int attempts = 0; attempts < MAX_ATTEMPTS; attempts++) {
            int x = EDGE_MARGIN + random.nextInt(Math.max(1, maxX - EDGE_MARGIN + 1));
            int y = EDGE_MARGIN + random.nextInt(Math.max(1, maxY - EDGE_MARGIN + 1));
            if (isValidCollectiblePosition(x, y, collectibleSize, solidObjects,
                    existingCollectibles, minDistanceFromSolid)) {
                return new int[] { x, y };
            }
        }

        for (int y = EDGE_MARGIN; y <= maxY; y += Math.max(10, collectibleSize / 2)) {
            for (int x = EDGE_MARGIN; x <= maxX; x += Math.max(10, collectibleSize / 2)) {
                if (isValidCollectiblePosition(x, y, collectibleSize, solidObjects,
                        existingCollectibles, minDistanceFromSolid)) {
                    System.out.println("Collectible spawn used grid fallback with preferred clearance");
                    return new int[] { x, y };
                }
            }
        }

        for (int y = EDGE_MARGIN; y <= maxY; y += Math.max(10, collectibleSize / 2)) {
            for (int x = EDGE_MARGIN; x <= maxX; x += Math.max(10, collectibleSize / 2)) {
                if (isValidCollectiblePosition(x, y, collectibleSize, solidObjects,
                        existingCollectibles, 0)) {
                    System.out.println("Collectible spawn relaxed distance check to avoid blocked placement");
                    return new int[] { x, y };
                }
            }
        }

        System.out.println("Warning: No safe collectible spawn position found; using edge margin fallback");
        return new int[] { EDGE_MARGIN, EDGE_MARGIN };
    }

    private boolean isValidCollectiblePosition(int x, int y,
            int collectibleSize,
            ArrayList<SolidObject> solidObjects,
            ArrayList<Collectible> existingCollectibles,
            int minDistanceFromSolid) {
        Rectangle2D.Double collectibleBounds = new Rectangle2D.Double(x, y, collectibleSize, collectibleSize);

        for (SolidObject solid : solidObjects) {
            Rectangle2D.Double solidBounds = solid.getBoundingRectangle();
            if (collectibleBounds.intersects(solidBounds)) {
                return false;
            }
            if (getDistanceBetweenRects(collectibleBounds, solidBounds) < minDistanceFromSolid) {
                return false;
            }
        }
        for (Collectible collectible : existingCollectibles) {
            if (collectible.isCollected())
                continue;
            Rectangle2D.Double existingBounds = collectible.getBoundingRectangle();
            if (collectibleBounds.intersects(existingBounds)
                    || getDistanceBetweenRects(collectibleBounds, existingBounds) < minDistanceFromSolid) {
                return false;
            }
        }
        return true;
    }

    private Collectible createCoinCollectible(int x, int y, int collectibleSize) {
        BufferedImage coinStrip = ImageManager.loadBufferedImage("images/coinStrip.png");
        if (coinStrip != null) {
            StripAnimation stripAnim = new StripAnimation(170, coinStrip.getHeight(), 18);
            stripAnim.setAnimationSpeed(60);
            BufferedImage[] coinFrames = stripAnim.extractFramesFromRow(coinStrip, 0);
            Animation coinAnimation = stripAnim.createAnimationFromFrames(coinFrames, 60, false);
            AnimatedSprite coinSprite = new AnimatedSprite(null, x, y, collectibleSize, collectibleSize);
            coinSprite.setAnimation(coinAnimation);
            return new Collectible(x, y, collectibleSize, collectibleSize, coinSprite);
        }
        return new Collectible(x, y, collectibleSize, collectibleSize, null);
    }

    public ArrayList<AnimatedSprite> createAnimatedSprites(javax.swing.JPanel panel) {
        ArrayList<AnimatedSprite> sprites = new ArrayList<AnimatedSprite>();
        AnimatedSprite sprite = new AnimatedSprite(panel, 500, 300, 50, 50);
        Animation anim = new Animation(true);
        anim.addFrame(ImageManager.loadImage("sprite1.png"), 200);
        anim.addFrame(ImageManager.loadImage("sprite2.png"), 200);
        sprite.setAnimation(anim);
        sprites.add(sprite);
        return sprites;
    }

    public double getDistanceFromRect(int x, int y, Rectangle2D.Double rect) {
        double closestX = Math.max(rect.getMinX(), Math.min(x, rect.getMaxX()));
        double closestY = Math.max(rect.getMinY(), Math.min(y, rect.getMaxY()));
        return Math.hypot(x - closestX, y - closestY);
    }

    private double getDistanceBetweenRects(Rectangle2D.Double first, Rectangle2D.Double second) {
        double dx = 0;
        if (first.getMaxX() < second.getMinX()) {
            dx = second.getMinX() - first.getMaxX();
        } else if (second.getMaxX() < first.getMinX()) {
            dx = first.getMinX() - second.getMaxX();
        }

        double dy = 0;
        if (first.getMaxY() < second.getMinY()) {
            dy = second.getMinY() - first.getMaxY();
        } else if (second.getMaxY() < first.getMinY()) {
            dy = first.getMinY() - second.getMaxY();
        }

        return Math.hypot(dx, dy);
    }
}

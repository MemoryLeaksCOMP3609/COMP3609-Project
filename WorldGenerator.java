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
    
    // Create a new WorldGenerator with the specified world dimensions.
    public WorldGenerator(int worldWidth, int worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.random = new Random();
    }
    
    public void loadImages() {
        // Load tree images scaled to max height 150px
        treeImages = ImageManager.loadTreeImages(150);
        
        // Load rock images scaled to max width 50px
        rockImages = ImageManager.loadRockImages(50);
        
        // Debug output
        int treesLoaded = 0;
        int rocksLoaded = 0;
        for (BufferedImage img : treeImages) {
            if (img != null) treesLoaded++;
        }
        for (BufferedImage img : rockImages) {
            if (img != null) rocksLoaded++;
        }
        System.out.println("Loaded " + treesLoaded + " tree images and " + rocksLoaded + " rock images");
    }
    
    /**
     * Create solid objects  at random positions
     * Avoids placing objects on top of player start position.
     */
    public ArrayList<SolidObject> createSolidObjects(int numObjects, int playerStartX, int playerStartY, int safeZoneRadius) {
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
                
                if (isTree) {
                    selectedImage = treeImages[random.nextInt(treeImages.length)];
                } else {
                    selectedImage = rockImages[random.nextInt(rockImages.length)];
                }
                
                if (selectedImage == null) {
                    break;
                }
                
                // Generate random position within world bounds
                int objX = EDGE_MARGIN + random.nextInt(worldWidth - 100);
                int objY = EDGE_MARGIN + random.nextInt(worldHeight - 100);
                
                // Check if position is safe (not too close to player start)
                double distToPlayer = Math.sqrt(
                    Math.pow(objX - playerStartX, 2) + 
                    Math.pow(objY - playerStartY, 2)
                );
                
                if (distToPlayer < safeZoneRadius) {
                    continue;
                }
                
                // Check for overlap with existing objects
                boolean overlaps = false;
                Rectangle2D.Double newBounds = new Rectangle2D.Double(
                    objX, objY, 
                    selectedImage.getWidth(), 
                    selectedImage.getHeight()
                );
                
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
            
            if (!objectPlaced) {
                System.out.println("Warning: Could not place object after " + MAX_ATTEMPTS_PER_OBJECT + " attempts");
            }
        }
        
        System.out.println("Created " + solids.size() + " random solid objects (trees and rocks)");
        return solids;
    }
    
    // reate collectibles at random positions, avoiding solid objects.
    public ArrayList<Collectible> createCollectibles(ArrayList<SolidObject> solidObjects, int numCollectibles, 
                                                       int collectibleSize, int minDistanceFromSolid) {
        ArrayList<Collectible> collectibles = new ArrayList<Collectible>();
        
        // Load coin strip image for animated collectibles
        BufferedImage coinStrip = ImageManager.loadBufferedImage("images/coinStrip.png");
        
        final int EDGE_MARGIN = 50;
        final int MAX_ATTEMPTS = 1000;
        
        // Generate random positions
        int[][] positions = new int[numCollectibles][2];
        
        for (int i = 0; i < numCollectibles; i++) {
            boolean validPosition = false;
            int attempts = 0;
            int x = 0, y = 0;
            
            while (!validPosition && attempts < MAX_ATTEMPTS) {
                attempts++;
                
                // Generate random position within world bounds
                x = EDGE_MARGIN + random.nextInt(worldWidth - 2 * EDGE_MARGIN - collectibleSize);
                y = EDGE_MARGIN + random.nextInt(worldHeight - 2 * EDGE_MARGIN - collectibleSize);
                
                validPosition = true;
                
                // Check distance from solid objects
                for (SolidObject solid : solidObjects) {
                    Rectangle2D.Double solidBounds = solid.getBoundingRectangle();
                    double distance = getDistanceFromRect(x, y, solidBounds);
                    
                    if (distance < minDistanceFromSolid) {
                        validPosition = false;
                        break;
                    }
                }
                
                // Check distance from other collectibles
                if (validPosition) {
                    for (int j = 0; j < i; j++) {
                        double dist = Math.sqrt(
                            Math.pow(x - positions[j][0], 2) + 
                            Math.pow(y - positions[j][1], 2)
                        );
                        if (dist < minDistanceFromSolid) {
                            validPosition = false;
                            break;
                        }
                    }
                }
            }
            
            if (validPosition) {
                positions[i][0] = x;
                positions[i][1] = y;
            } else {
                positions[i][0] = EDGE_MARGIN + random.nextInt(worldWidth - 2 * EDGE_MARGIN);
                positions[i][1] = EDGE_MARGIN + random.nextInt(worldHeight - 2 * EDGE_MARGIN);
                System.out.println("Warning: Could not find valid position for collectible " + i + " after " + MAX_ATTEMPTS + " attempts");
            }
        }
        
        if (coinStrip != null) {
            System.out.println("Coin strip loaded: " + coinStrip.getWidth() + "x" + coinStrip.getHeight());
            
            // Create animated collectibles
            StripAnimation stripAnim = new StripAnimation(170, coinStrip.getHeight(), 18);
            stripAnim.setAnimationSpeed(60);
            
            BufferedImage[] coinFrames = stripAnim.extractFramesFromRow(coinStrip, 0);
            System.out.println("Extracted " + coinFrames.length + " coin frames");
            
            Animation coinAnimation = stripAnim.createAnimationFromFrames(coinFrames, 60, false);
            
            // Create animated sprites at the randomly generated positions
            for (int i = 0; i < positions.length; i++) {
                AnimatedSprite coinSprite = new AnimatedSprite(null, positions[i][0], positions[i][1], collectibleSize, collectibleSize);
                coinSprite.setAnimation(coinAnimation);
                
                collectibles.add(new Collectible(positions[i][0], positions[i][1], collectibleSize, collectibleSize, coinSprite));
            }
            
            System.out.println("Created " + collectibles.size() + " animated coin collectibles at random positions");
        } else {
            System.out.println("Failed to load coinStrip.png, creating collectibles with placeholder sprites");
            for (int i = 0; i < positions.length; i++) {
                collectibles.add(new Collectible(positions[i][0], positions[i][1], collectibleSize, collectibleSize, null));
            }
        }
        
        return collectibles;
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
        
        double distance = Math.sqrt(Math.pow(x - closestX, 2) + Math.pow(y - closestY, 2));
        
        return distance;
    }
}

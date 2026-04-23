import java.awt.Image;
import java.awt.Graphics2D;
import java.util.HashMap;
import javax.swing.ImageIcon;

public class TileMap {

    private static final int TILE_SIZE = 100;
    private static final int TILE_SIZE_BITS = 6;
    private static final int TILE_INDEX_OFFSET = -1;
    private int currentLevel = 1;

    private int[][] tileIndices;
    private HashMap<Integer, Image> imageCache;
    private String terrainType;
    private int mapWidth;
    private int mapHeight;

    public TileMap(int width, int height, String terrainType) {
        this.mapWidth = width;
        this.mapHeight = height;
        this.terrainType = terrainType;
        this.tileIndices = new int[width][height];
        this.imageCache = new HashMap<>();

        // Initialize all tiles to -1 (empty)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tileIndices[x][y] = -1;
            }
        }

        System.out.println("Created TileMap: " + width + "x" + height + " (" + terrainType + ")");
    }

    public void setCurrentLevel(int level) {
        this.currentLevel = level;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setTileIndex(int x, int y, int tileIndex) {
        if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
            tileIndices[x][y] = tileIndex;
        }
    }

    public int getTileIndex(int x, int y) {
        if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) {
            return -1;
        }
        return tileIndices[x][y];
    }

    public Image getTile(int x, int y) {
        int tileIndex = getTileIndex(x, y);
        if (tileIndex < 0)
            return null;

        int adjustedIndex = tileIndex + TILE_INDEX_OFFSET; // ← apply offset here
        if (adjustedIndex < 0)
            return null;

        if (imageCache.containsKey(adjustedIndex)) {
            return imageCache.get(adjustedIndex);
        }

        String filename = "images/tiles/" + terrainType + "/" + terrainType + "Tile_" + adjustedIndex + ".png";
        Image tileImage = new ImageIcon(filename).getImage();
        imageCache.put(adjustedIndex, tileImage);
        return tileImage;
    }

    public int getWidth() {
        return mapWidth;
    }

    public int getHeight() {
        return mapHeight;
    }

    public int getWidthPixels() {
        return tilesToPixels(mapWidth);
    }

    public int getHeightPixels() {
        return tilesToPixels(mapHeight);
    }

    public String getTerrainType() {
        return terrainType;
    }

    public static int pixelsToTiles(float pixels) {
        return pixelsToTiles(Math.round(pixels));
    }

    public static int pixelsToTiles(int pixels) {
        return (int) Math.floor((float) pixels / TILE_SIZE);
    }

    public static int tilesToPixels(int numTiles) {
        return numTiles * TILE_SIZE;
    }

    public static int getTileSize() {
        return TILE_SIZE;
    }

    /**
     * Draws the map tiles to the graphics context.
     * 
     * @param g2           Graphics2D context
     * @param offsetX      X offset in pixels
     * @param offsetY      Y offset in pixels
     * @param screenWidth  Screen width in pixels
     * @param screenHeight Screen height in pixels
     */

    public void draw(Graphics2D g2, int offsetX, int offsetY, int screenWidth, int screenHeight) {
        int firstTileX = Math.max(0, pixelsToTiles(-offsetX));
        int lastTileX = Math.min(mapWidth - 1, firstTileX + pixelsToTiles(screenWidth) + 1);

        int firstTileY = Math.max(0, pixelsToTiles(-offsetY)); // ← was missing
        int lastTileY = Math.min(mapHeight - 1, firstTileY + pixelsToTiles(screenHeight) + 1); // ← was missing

        for (int y = firstTileY; y <= lastTileY; y++) { // ← was: for y=0 to mapHeight
            for (int x = firstTileX; x <= lastTileX; x++) {
                Image image = getTile(x, y);
                if (image != null) {
                    g2.drawImage(image,
                            tilesToPixels(x) + offsetX,
                            tilesToPixels(y) + offsetY, // ← offsetY was missing here
                            TILE_SIZE, TILE_SIZE,
                            null);
                }
            }
        }
    }

    // checks if a world pixel position is a blocked tile
    public boolean isSolid(int worldX, int worldY) {
        int tileX = pixelsToTiles(worldX);
        int tileY = pixelsToTiles(worldY);

        // Out of MAP bounds (not screen bounds) — block at world edge
        if (tileX < 0 || tileX >= mapWidth || tileY < 0 || tileY >= mapHeight) {
            return true;
        }

        int index = getTileIndex(tileX, tileY);

        // -1 means out of bounds — treated as a solid wall so player can't leave the
        // map
        if (index == -1)
            return false;

        // un-walkable tiles: 44 = grass, 56 = desert/ice

        // decided to allow player to leave the path (but leaving code in case need to
        // limit player again)
        // so, adding a 0 to every flag (since there is no tile past 55) —
        // this is a bit hacky but it works with the current tile sets
        switch (currentLevel) {
            case 2:
                return index == 560;
            case 3:
                return index == 560;
            default:
                return index == 440;
        }
    }

}

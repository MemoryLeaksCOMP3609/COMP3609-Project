import java.awt.Image;
import java.awt.Graphics2D;
import java.util.HashMap;
import javax.swing.ImageIcon;

public class TileMap {

    private static final int TILE_SIZE = 100;
    private static final int TILE_SIZE_BITS = 6;

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
        if (tileIndex < 0) {
            return null;
        }

        if (imageCache.containsKey(tileIndex)) {
            return imageCache.get(tileIndex);
        }

        String filename = "images/tiles/" + terrainType + "/" + terrainType + "Tile_" + tileIndex + ".png";
        Image tileImage = new ImageIcon(filename).getImage();
        imageCache.put(tileIndex, tileImage);

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
        int firstTileX = pixelsToTiles(-offsetX);
        int lastTileX = firstTileX + pixelsToTiles(screenWidth) + 1;

        for (int y = 0; y < mapHeight; y++) {
            for (int x = firstTileX; x <= lastTileX; x++) {
                Image image = getTile(x, y);
                if (image != null) {
                    g2.drawImage(image,
                            tilesToPixels(x) + offsetX,
                            tilesToPixels(y) + offsetY,
                            null);
                }
            }
        }
    }
}

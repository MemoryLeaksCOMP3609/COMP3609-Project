import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class TileMapManager {

    private HashMap<String, ArrayList<Integer>> tileCache;

    public TileMapManager() {
        this.tileCache = new HashMap<>();
    }

    public TileMap loadMap(String filename, String terrainType)
            throws IOException {
        ArrayList<String> lines = new ArrayList<String>();
        int mapWidth = 0;
        int mapHeight = 0;

        BufferedReader reader = new BufferedReader(new FileReader(filename));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                reader.close();
                break;
            }

            // Add every line except for comments
            if (!line.startsWith("#")) {
                lines.add(line);
                // Count max width based on comma-separated values
                int tileCount = line.split(",").length;
                mapWidth = Math.max(mapWidth, tileCount);
            }
        }

        mapHeight = lines.size();
        System.out.println("Loaded map: " + filename + " (" + mapWidth + "x" + mapHeight + ") - " + terrainType);

        TileMap newMap = new TileMap(mapWidth, mapHeight, terrainType);

        for (int y = 0; y < mapHeight; y++) {
            String line = lines.get(y);
            String[] tokens = line.split(",");
            for (int x = 0; x < tokens.length; x++) {
                try {
                    int tileIndex = Integer.parseInt(tokens[x].trim());
                    newMap.setTileIndex(x, y, tileIndex);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid tile index at (" + x + "," + y + "): " + tokens[x]);
                }
            }
        }

        return newMap;
    }

    public ArrayList<Integer> loadTerrainTiles(String terrainType) {
        ArrayList<Integer> tileIndices = new ArrayList<Integer>();

        if (tileCache.containsKey(terrainType)) {
            return tileCache.get(terrainType);
        }

        System.out.println("Loading " + terrainType + " tiles...");

        int index = 0;
        while (true) {
            String filename = "images/tiles/" + terrainType + "/" + terrainType + "Tile_" + index + ".png";
            File file = new File(filename);
            if (!file.exists()) {
                System.out.println("Loaded " + index + " " + terrainType + " tiles.");
                break;
            }
            tileIndices.add(index);
            System.out.println("Found: " + filename);
            index++;
        }

        tileCache.put(terrainType, tileIndices);
        return tileIndices;
    }
}

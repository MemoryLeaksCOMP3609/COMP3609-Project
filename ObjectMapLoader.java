import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ObjectMapLoader {

    private static final int TILE_SIZE = 100;

    private final HashMap<String, BufferedImage> imageCache = new HashMap<>();

    /**
     * Loads an object map and returns SolidObjects.
     * 
     * @param filename
     * @param region
     */
    public ArrayList<SolidObject> loadObjects(String filename, String region) {
        ArrayList<SolidObject> objects = new ArrayList<>();

        ArrayList<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("#") && !line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("ObjectMapLoader: could not read '" + filename + "': " + e.getMessage());
            return objects;
        }

        int placed = 0;

        for (int row = 0; row < lines.size(); row++) {
            String[] tokens = lines.get(row).split(" ");
            for (int col = 0; col < tokens.length; col++) {
                String raw = tokens[col].trim();
                if (raw.isEmpty())
                    continue;

                int id;
                try {
                    id = Integer.parseInt(raw);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (id == 0)
                    continue; // 0 = empty, skip

                // build image path from region and id
                String imagePath = "images/chosen_objects/" + region + "/" + id + ".png";

                BufferedImage img = imageCache.computeIfAbsent(
                        imagePath, ImageManager::loadBufferedImage);

                if (img == null) {
                    System.out.println("ObjectMapLoader: failed to load " + imagePath);
                    continue;
                }

                // centre horizontally, bottom-anchor vertically within tile
                int tilePixelX = col * TILE_SIZE;
                int tilePixelY = row * TILE_SIZE;
                int objX = tilePixelX + (TILE_SIZE - img.getWidth()) / 2;
                int objY = Math.max(0, tilePixelY + (TILE_SIZE - img.getHeight()));

                objects.add(new SolidObject(objX, objY, img));
                placed++;
            }
        }

        System.out.println("ObjectMapLoader: placed " + placed + " objects from '" + filename + "'");
        return objects;
    }
}
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class PixelCollision {

    private PixelCollision() {
    }

    public static boolean intersects(Rectangle2D.Double boundsA, BufferedImage imageA,
                                     Rectangle2D.Double boundsB, BufferedImage imageB) {
        return countOverlappingPixels(boundsA, imageA, boundsB, imageB, 1) > 0;
    }

    public static int countOverlappingPixels(Rectangle2D.Double boundsA, BufferedImage imageA,
                                             Rectangle2D.Double boundsB, BufferedImage imageB,
                                             int stopAfter) {
        if (boundsA == null || boundsB == null) {
            return 0;
        }

        if (!boundsA.intersects(boundsB)) {
            return 0;
        }

        if (imageA == null || imageB == null) {
            return 1;
        }

        int startX = (int) Math.max(boundsA.x, boundsB.x);
        int startY = (int) Math.max(boundsA.y, boundsB.y);
        int endX = (int) Math.min(boundsA.x + boundsA.width, boundsB.x + boundsB.width);
        int endY = (int) Math.min(boundsA.y + boundsA.height, boundsB.y + boundsB.height);
        int overlapCount = 0;

        for (int worldY = startY; worldY < endY; worldY++) {
            int imageAY = toImageCoordinate(worldY, boundsA.y, boundsA.height, imageA.getHeight());
            int imageBY = toImageCoordinate(worldY, boundsB.y, boundsB.height, imageB.getHeight());

            for (int worldX = startX; worldX < endX; worldX++) {
                int imageAX = toImageCoordinate(worldX, boundsA.x, boundsA.width, imageA.getWidth());
                int imageBX = toImageCoordinate(worldX, boundsB.x, boundsB.width, imageB.getWidth());

                if (isOpaque(imageA, imageAX, imageAY) && isOpaque(imageB, imageBX, imageBY)) {
                    overlapCount++;
                    if (overlapCount >= stopAfter) {
                        return overlapCount;
                    }
                }
            }
        }

        return overlapCount;
    }

    public static BufferedImage toBufferedImage(Image image) {
        if (image == null) {
            return null;
        }

        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        int width = image.getWidth(null);
        int height = image.getHeight(null);

        if (width <= 0 || height <= 0) {
            return null;
        }

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        bufferedImage.getGraphics().drawImage(image, 0, 0, null);
        return bufferedImage;
    }

    private static int toImageCoordinate(int worldCoordinate, double boundsStart, double boundsSize, int imageSize) {
        if (boundsSize <= 0 || imageSize <= 0) {
            return 0;
        }

        double normalized = (worldCoordinate - boundsStart) / boundsSize;
        int imageCoordinate = (int) (normalized * imageSize);

        if (imageCoordinate < 0) {
            return 0;
        }

        return Math.min(imageCoordinate, imageSize - 1);
    }

    private static boolean isOpaque(BufferedImage image, int x, int y) {
        int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
        return alpha > 0;
    }
}

import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class PlayerCollisionResolver {
    private final int maxPushOutDistance;

    public PlayerCollisionResolver(int maxPushOutDistance) {
        this.maxPushOutDistance = maxPushOutDistance;
    }

    public boolean resolve(PlayerSprite player, ArrayList<SolidObject> solidObjects,
            int fallbackX, int fallbackY, int moveDirection, TileMap tileMap) {

        Rectangle2D.Double playerBounds = player.getBoundingRectangle();
        BufferedImage playerImage = player.getCurrentCollisionMaskImage();

        for (SolidObject solid : solidObjects) {
            if (PixelCollision.intersects(playerBounds, playerImage,
                    solid.getBoundingRectangle(), solid.getImage())) {
                return pushPlayerOutOfSolid(player, solid, fallbackX, fallbackY, moveDirection);
            }
        }

        // tilemap collision check
        if (tileMap != null) {
            int px = player.getWorldX();
            int py = player.getWorldY();
            int pw = (int) playerBounds.getWidth();
            int ph = (int) playerBounds.getHeight();

            // check all 4 corners of the player's bounding box
            if (tileMap.isSolid(px, py) ||
                    tileMap.isSolid(px + pw, py) ||
                    tileMap.isSolid(px, py + ph) ||
                    tileMap.isSolid(px + pw, py + ph)) {
                player.setWorldX(fallbackX);
                player.setWorldY(fallbackY);
                return false;
            }
        }

        return true;
    }

    private boolean pushPlayerOutOfSolid(PlayerSprite player, SolidObject solid,
            int fallbackX, int fallbackY, int moveDirection) {
        int originalX = player.getWorldX();
        int originalY = player.getWorldY();
        int[] reverseDirection = getReverseStep(moveDirection);

        if (reverseDirection[0] == 0 && reverseDirection[1] == 0) {
            return placePlayerAt(player, fallbackX, fallbackY);
        }

        for (int step = 1; step <= maxPushOutDistance; step++) {
            int candidateX = originalX + reverseDirection[0] * step;
            int candidateY = originalY + reverseDirection[1] * step;

            if (placePlayerAt(player, candidateX, candidateY) && !isPlayerCollidingWithSolid(player, solid)) {
                return true;
            }
        }

        return placePlayerAt(player, fallbackX, fallbackY);
    }

    private boolean isPlayerCollidingWithSolid(PlayerSprite player, SolidObject solid) {
        return PixelCollision.intersects(player.getBoundingRectangle(), player.getCurrentCollisionMaskImage(),
                solid.getBoundingRectangle(), solid.getImage());
    }

    private boolean placePlayerAt(PlayerSprite player, int worldX, int worldY) {
        player.setWorldX(worldX);
        player.setWorldY(worldY);
        return true;
    }

    private int[] getReverseStep(int moveDirection) {
        switch (moveDirection) {
            case PlayerSprite.DIR_LEFT:
                return new int[] { 1, 0 };
            case PlayerSprite.DIR_RIGHT:
                return new int[] { -1, 0 };
            case PlayerSprite.DIR_UP:
                return new int[] { 0, 1 };
            case PlayerSprite.DIR_DOWN:
                return new int[] { 0, -1 };
            case PlayerSprite.DIR_UP_LEFT:
                return new int[] { 1, 1 };
            case PlayerSprite.DIR_UP_RIGHT:
                return new int[] { -1, 1 };
            case PlayerSprite.DIR_DOWN_LEFT:
                return new int[] { 1, -1 };
            case PlayerSprite.DIR_DOWN_RIGHT:
                return new int[] { -1, -1 };
            default:
                return new int[] { 0, 0 };
        }
    }
}

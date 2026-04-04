public class GameInputState {
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean upPressed;
    private boolean downPressed;

    public void setLeftPressed(boolean pressed) {
        leftPressed = pressed;
    }

    public void setRightPressed(boolean pressed) {
        rightPressed = pressed;
    }

    public void setUpPressed(boolean pressed) {
        upPressed = pressed;
    }

    public void setDownPressed(boolean pressed) {
        downPressed = pressed;
    }

    public boolean isAnyMovementPressed() {
        return leftPressed || rightPressed || upPressed || downPressed;
    }

    public int resolveMovementDirection() {
        if (upPressed && leftPressed) {
            return PlayerSprite.DIR_UP_LEFT;
        }
        if (upPressed && rightPressed) {
            return PlayerSprite.DIR_UP_RIGHT;
        }
        if (downPressed && leftPressed) {
            return PlayerSprite.DIR_DOWN_LEFT;
        }
        if (downPressed && rightPressed) {
            return PlayerSprite.DIR_DOWN_RIGHT;
        }
        if (leftPressed && !rightPressed) {
            return PlayerSprite.DIR_LEFT;
        }
        if (rightPressed && !leftPressed) {
            return PlayerSprite.DIR_RIGHT;
        }
        if (upPressed && !downPressed) {
            return PlayerSprite.DIR_UP;
        }
        if (downPressed && !upPressed) {
            return PlayerSprite.DIR_DOWN;
        }
        return 0;
    }
}

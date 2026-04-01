import java.awt.Graphics2D;
import javax.swing.JPanel;

/**
 * Animated sprite using sprite sheet or multiple frames.
 * Provides frame-based animation support.
 */
public class AnimatedSprite extends Sprite {
    
    private Animation animation;
    private int screenX;
    private int screenY;
    private boolean active;
    
    public AnimatedSprite(JPanel p, int xPos, int yPos, int w, int h) {
        super(p, xPos, yPos, w, h);
        screenX = xPos;
        screenY = yPos;
        animation = new Animation();
        active = true;
    }
    
    public void setAnimation(Animation anim) {
        animation = anim;
        animation.start();
    }
    
    public void update() {
        if (animation != null && active) {
            animation.update();
        }
    }
    
    public void updateScreenPosition(int cameraX, int cameraY) {
        screenX = x - cameraX;
        screenY = y - cameraY;
    }
    
    @Override
    public void draw(Graphics2D g2) {
        if (!active) return;
        
        if (animation != null && animation.getImage() != null) {
            g2.drawImage(animation.getImage(), screenX, screenY, width, height, null);
        } else if (image != null) {
            g2.drawImage(image, screenX, screenY, width, height, null);
        }
    }
    
}

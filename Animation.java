import java.awt.Image;
import java.util.ArrayList;

/**
 * Animation utility class for frame-based animation.
 */
public class Animation {
    
    private ArrayList<AnimFrame> frames;
    private int currFrameIndex;
    private long animTime;
    private long startTime;
    private long totalDuration;
    private boolean loop;
    private boolean isActive;
    
    public Animation() {
        frames = new ArrayList<AnimFrame>();
        totalDuration = 0;
        loop = true;
        isActive = false;
    }
    
    public Animation(boolean loop) {
        frames = new ArrayList<AnimFrame>();
        totalDuration = 0;
        this.loop = loop;
        isActive = false;
    }
    
    public synchronized void addFrame(Image image, long duration) {
        totalDuration += duration;
        frames.add(new AnimFrame(image, totalDuration));
    }
    
    public synchronized void start() {
        isActive = true;
        animTime = 0;
        currFrameIndex = 0;
        startTime = System.currentTimeMillis();
    }
    
    public synchronized void update() {
        if (!isActive || frames.size() <= 1)
            return;
        
        long currTime = System.currentTimeMillis();
        long elapsedTime = currTime - startTime;
        startTime = currTime;
        
        animTime += elapsedTime;
        
        if (animTime >= totalDuration) {
            if (loop) {
                animTime = animTime % totalDuration;
                currFrameIndex = 0;
            } else {
                isActive = false;
                return;
            }
        }
        
        while (animTime > getFrame(currFrameIndex).endTime) {
            currFrameIndex++;
        }
    }
    
    public synchronized Image getImage() {
        if (frames.size() == 0) {
            return null;
        }
        return getFrame(currFrameIndex).image;
    }
    
    private AnimFrame getFrame(int i) {
        return frames.get(i);
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    private class AnimFrame {
        Image image;
        long endTime;
        
        AnimFrame(Image image, long endTime) {
            this.image = image;
            this.endTime = endTime;
        }
    }
}

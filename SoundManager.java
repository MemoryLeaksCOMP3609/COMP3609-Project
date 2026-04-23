import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import java.io.File;
import java.util.HashMap;

// Sound clip management with singleton pattern.

public class SoundManager {

    private static SoundManager instance = null;
    public HashMap<String, Clip> clips;

    private SoundManager() {
        clips = new HashMap<String, Clip>();

        // Load sound clips
        loadClip("footstep", "sounds/runningOnGrass.wav");
        loadClip("coinPickup", "sounds/coinPickp.wav");
        loadClip("background", "sounds/backgroundMusic.wav");
        loadClip("grass-footsteps", "sounds/grass-footsteps.wav");
        loadClip("sand-footsteps", "sounds/sand-footsteps.wav");
        loadClip("snow-footsteps", "sounds/snow-footsteps.wav");
        loadClip("path-footsteps", "sounds/path-footsteps.wav");
        loadClip("boss-footsteps", "sounds/boss-footsteps.wav");
        loadClip("coinPickup", "sounds/coinPickp.wav");
        loadClip("grass-background", "sounds/grass-background.wav");
        loadClip("sand-background", "sounds/sand-background.wav");
        loadClip("snow-background", "sounds/snow-background.wav");
        loadClip("grass-background-music", "sounds/grass-background.wav");
        loadClip("sand-background-music", "sounds/sand-background.wav");
        loadClip("snow-background-music", "sounds/snow-background.wav");
        loadClip("player-hurt", "sounds/player-hurt.wav");
        loadClip("player-dies", "sounds/player-dies.wav");
        loadClip("spell-attack-noise", "sounds/spell-attack-noise.wav");
        loadClip("enemy-hurt", "sounds/enemy-hurt.wav");
        loadClip("portal-sound", "sounds/portal-sound.wav");
        loadClip("crystal-sound", "sounds/crystal-sound.wav");
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    private void loadClip(String name, String filename) {
        try {
            File file = new File(filename);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(file.toURI().toURL());
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clips.put(name, clip);
        } catch (Exception e) {
            System.out.println("Error loading sound: " + filename + " - " + e);
        }
    }

    public void playClip(String name, boolean looping) {
        Clip clip = clips.get(name);
        if (clip != null) {
            clip.setFramePosition(0);
            if (looping) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                clip.start();
            }
        }
    }

    public void stopClip(String name) {
        Clip clip = clips.get(name);
        if (clip != null) {
            clip.stop();
        }
    }

    public void stopAll() {
        for (Clip clip : clips.values()) {
            clip.stop();
        }
    }

    public boolean isPlaying(String name) {
        Clip clip = clips.get(name);
        return clip != null && clip.isRunning();
    }

    public void playFootstep(String clipName) {
        // Stop any currently playing footstep
        stopClip("grass-footsteps");
        stopClip("sand-footsteps");
        stopClip("snow-footsteps");
        stopClip("path-footsteps");

        playClip(clipName, false);
    }

    public void stopFootstep() {
        stopClip("grass-footsteps");
        stopClip("sand-footsteps");
        stopClip("snow-footsteps");
        stopClip("path-footsteps");
    }

    public void playBackgroundMusic(String terrain) {
        stopClip("grass-background");
        stopClip("sand-background");
        stopClip("snow-background");

        String clipName = terrain.equals("desert") ? "sand-background"
                : terrain.equals("ice") ? "snow-background"
                        : "grass-background";

        Clip clip = clips.get(clipName);
        if (clip != null) {
            FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            if (volumeControl != null) {
                float dB = (float) (20 * Math.log10(0.6f));
                volumeControl.setValue(dB);
            }
            clip.setFramePosition(0);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void playBackgroundMusic() {
        playBackgroundMusic("grass");
    }
}

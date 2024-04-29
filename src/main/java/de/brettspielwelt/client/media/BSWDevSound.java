package de.brettspielwelt.client.media;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;

public class BSWDevSound {

	private static BSWDevSound instance;

	private final AutocloseMixer autoCloseMixer;
	private boolean mixerOpen;

	private BSWDevSound() {
		autoCloseMixer = new AutocloseMixer();
		try {
			autoCloseMixer.open();
			mixerOpen = true;
		} catch (LineUnavailableException ignored) {
			mixerOpen = false;
		}
	}

	public static synchronized BSWDevSound instance() {
		if (instance == null) {
			instance = new BSWDevSound();
		}
		return instance;
	}

	public Clip getClip(InputStream inputStream) throws IOException, UnsupportedAudioFileException,
			LineUnavailableException {
		if (mixerOpen) {
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
			Line.Info info = new DataLine.Info(Clip.class, audioInputStream.getFormat());
			Clip clip = (Clip) autoCloseMixer.getLine(info);
			clip.open(audioInputStream);
			return clip;
		} else {
			return null;
		}
	}

}

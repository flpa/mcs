package at.fhtw.mcs.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import at.fhtw.mcs.model.TrackFactory.UnsupportedFormatException;
import at.fhtw.mcs.util.FormatDetection;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;

public class JavaxJavazoomTrack implements Track {
	private String path;
	private Clip clip;
	private int framePosition = 0;

	/**
	 * Creates the track using the given {@link FormatDetection}.
	 * 
	 * @throws UnsupportedFormatException
	 *             In case the format of the track is not supported.
	 */
	public JavaxJavazoomTrack(FormatDetection formatDetection, String path) {
		Format format = formatDetection.detectFormat(path);

		switch (format) {
			case AIFF:
			case WAV:
				this.path = path;
				break;
			case MP3:
				this.path = convertMp3ToWav(path);
				break;
			default:
				throw new UnsupportedFormatException(format);
		}

		clip = openClip();
	}

	private String convertMp3ToWav(String path) {
		// TODO uppercase, e.g. MP3
		// TODO test: josh.new.mp3.mp3
		String newPath;
		Converter converter = new Converter();

		int positionOfMp3 = path.lastIndexOf(".mp3");
		newPath = path.substring(0, positionOfMp3) + ".wav";
		try {
			converter.convert(path, newPath);
		} catch (JavaLayerException e) {
			throw new UnsupportedFormatException(Format.MP3,
					"There was an error while converting the MP3 to WAV. Try consulting JavaZoom documentation.", e);
		}

		return newPath;
	}

	private Clip openClip() throws RuntimeException {
		try {
			URL url = new File(path).toURI().toURL();
			AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
			Clip clip = AudioSystem.getClip();
			clip.open(audioIn);
			return clip;
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			throw new RuntimeException("Error while opening clip", e);
		}
	}

	@Override
	public void play() {
		clip.setFramePosition(framePosition);
		clip.start();
	}

	@Override
	public void pause() {
		framePosition = clip.getFramePosition();
		clip.stop();
	}

	@Override
	public void togglePlayPause() {
		if (clip.isRunning()) {
			pause();
		} else {
			play();
		}
	}

	@Override
	public void stop() {
		clip.stop();
		framePosition = 0;
	}
}

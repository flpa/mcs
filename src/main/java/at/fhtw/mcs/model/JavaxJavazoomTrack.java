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
					"There was an error while converting the MP3 to WAV. Try consulting JavaZoom documentation.");
		}

		return newPath;
	}

	@Override
	public void play() {
		try {
			URL url = new File(path).toURI().toURL();

			AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);

			// Get a sound clip resource.
			Clip clip = AudioSystem.getClip();

			// Open audio clip and load samples from the audio input stream.
			clip.open(audioIn);
			clip.start();

		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void togglePlayPause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}

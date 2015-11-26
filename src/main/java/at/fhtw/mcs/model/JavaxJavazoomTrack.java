package at.fhtw.mcs.model;

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
		// TODO Auto-generated method stub

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

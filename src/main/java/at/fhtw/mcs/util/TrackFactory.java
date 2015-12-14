package at.fhtw.mcs.util;

import javax.sound.sampled.AudioFormat;

import at.fhtw.mcs.model.Format;
import at.fhtw.mcs.model.JavaxJavazoomTrack;
import at.fhtw.mcs.model.Track;

/**
 * Helper class to instatiate {@link Track}s.
 */
public class TrackFactory {
	private TrackFactory() {
		// no instances allowed
	}

	/**
	 * Loads a {@link Track} from the given relative path.
	 * 
	 * @throws UnsupportedFormatException
	 *             In case the format of the track is not supported.
	 */
	public static Track loadTrack(String path) throws UnsupportedFormatException {
		return new JavaxJavazoomTrack(new ExtensionFormatDetection(), path);
	}

	public static class UnsupportedFormatException extends IllegalArgumentException {
		private static final long serialVersionUID = -1933962444432614242L;

		private Format format;
		private AudioFormat audioFormat;

		public UnsupportedFormatException(Format format) {
			this.format = format;
		}

		public UnsupportedFormatException(Format format, AudioFormat audioFormat) {
			this.format = format;
			this.audioFormat = audioFormat;
		}

		public UnsupportedFormatException(String message) {
			super(message);
		}

		public UnsupportedFormatException(Format format, String message) {
			super(message);
			this.format = format;
		}

		public UnsupportedFormatException(Format format, String message, Exception e) {
			super(message, e);
			this.format = format;
		}

		public Format getFormat() {
			return format;
		}

		public AudioFormat getAudioFormat() {
			return audioFormat;
		}
	}
}

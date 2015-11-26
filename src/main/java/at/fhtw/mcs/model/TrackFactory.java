package at.fhtw.mcs.model;

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
		return null;
	}

	public static class UnsupportedFormatException extends Exception {
		private static final long serialVersionUID = -1933962444432614242L;

		private String format;

		public UnsupportedFormatException(String format) {
			this.format = format;
		}

		public String getFormat() {
			return format;
		}
	}
}

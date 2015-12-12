package at.fhtw.mcs.model;

import java.util.Vector;

/**
 * Defines the general interface of an audio track.
 * 
 * @author florian.patzl@technikum-wien.at
 */
public interface Track {

	/**
	 * Starts playback. Does nothing, if playback is already running.
	 */
	void play();

	/**
	 * Pauses playback. Does nothing, if playback is not running.
	 */
	void pause();

	/**
	 * Starts playback if not running. Pauses playback if running.
	 */
	void togglePlayPause();

	/**
	 * Stops playback, resetting the position. That means that invoking
	 * {@link #play()} after invoking this method, playback is started from the
	 * start.
	 */
	void stop();

	/**
	 * @return the current position in microseconds
	 * @see Track#getTotalMicroseconds()
	 */
	long getCurrentMicroseconds();

	/**
	 * @return the total length in microseconds.
	 */
	long getTotalMicroseconds();

	/**
	 * @return the average Loudness of the Track in dB
	 */
	void calculateLoudness();

	/**
	 * @return the name of the File
	 */
	String getFilename();

	/**
	 * @return the Amplitudedata of each Sample of the audiofile
	 */
	Vector<float[]> getAudioData();

	/**
	 * @return the length of the audiofile in Samples
	 */
	int getLength();

	/**
	 * @return the number of Channels of the audiofile
	 */
	int getNumberOfChannels();
}

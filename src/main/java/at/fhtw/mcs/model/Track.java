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
	 * @return the dB Value of a float (-1.0 to 1.0)
	 */
	float floatToDecibel(float sample);

	/**
	 * @return a float (-1.0 to 1.0) of a dB-Value
	 */
	float decibelToFloat(float dB);

	/**
	 * @return the average Loudness of the Track in dB
	 */
	void calculateLoudness();

	String getFilename();

	Vector<float[]> getAudioData();

	int getLength();
}

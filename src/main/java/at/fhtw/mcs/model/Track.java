package at.fhtw.mcs.model;

import java.util.Vector;

/**
 * Defines the general interface of an audio track.
 * 
 * @author florian.patzl@technikum-wien.at
 */
public interface Track {

	/**
	 * Starts playback. Does nothing if playback is already running.
	 */
	void play();

	/**
	 * Pauses playback. Does nothing if playback is not running.
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

	void setCurrentMicroseconds(long currentMicroseconds);

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

	/**
	 * Reloads the Track. Does *not* repeat audio analysis.
	 */
	void reload();

	/**
	 * Mutes the track: It continues playback if it was playing before but can
	 * no longer be heard.
	 */
	void mute();

	/**
	 * Unmutes the track, so that it can be heard.
	 */
	void unmute();

	boolean isMuted();

	float getLoudness();

	void setVolume(float lowest);

	boolean isPlaying();
}

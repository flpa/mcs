package at.fhtw.mcs.model;

import java.io.File;
import java.io.IOException;

import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;

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

	String getName();

	void setName(String name);

	String getFileName();

	String getPath();

	void saveAs(File destination) throws IOException;

	/**
	 * @return the Amplitudedata of each Sample of the audiofile
	 */
	float[] getAudioData();

	/**
	 * @return the length of the audiofile in Samples
	 */
	int getLength();

	/**
	 * @return in weighted Samples, so 48000Hz Files have the same length as
	 *         441000Hz files
	 */
	int getLengthWeighted();

	/**
	 * @return the number of Channels of the audiofile
	 */
	int getNumberOfChannels();

	/**
	 * Reloads the Track. Does *not* repeat audio analysis.
	 */
	void reload();

	float getLoudness();

	/**
	 * @return dynamic range of track (as difference between peak and rms) in dB
	 */
	float getDynamicRange();

	void setVolume(float lowest);

	void changeVolume(double delta);

	boolean isPlaying();

	/**
	 * @return comment of the track
	 */
	String getComment();

	void setComment(String comment);

	void applyStartPointOffset();

	void resetStartPointOffset();

	long getStartPointOffset();

	void registerCommentChangeListener(ChangeListener<? super String> listener);

	float getSampleRate();

	Property<String> commentProperty();
}

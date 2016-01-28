package at.fhtw.mcs.model;

import java.io.File;
import java.io.IOException;

import javafx.beans.value.ChangeListener;

/**
 * Dummy {@link Track} implementation that does nothing. Intended as a base
 * class for test Tracks in unit tests.
 *
 */
public class NoOpTrack implements Track {

	@Override
	public void play() {
		// no op
		// no op

	}

	@Override
	public void pause() {
		// no op

	}

	@Override
	public void togglePlayPause() {
		// no op

	}

	@Override
	public void stop() {
		// no op

	}

	@Override
	public long getCurrentMicroseconds() {
		// no op
		return 0;
	}

	@Override
	public void setCurrentMicroseconds(long currentMicroseconds) {
		// no op

	}

	@Override
	public long getTotalMicroseconds() {
		// no op
		return 0;
	}

	@Override
	public String getName() {
		// no op
		return null;
	}

	@Override
	public void setName(String name) {
		// no op

	}

	@Override
	public String getFileName() {
		// no op
		return null;
	}

	@Override
	public String getPath() {
		// no op
		return null;
	}

	@Override
	public void saveAs(File destination) throws IOException {
		// no op

	}

	@Override
	public float[] getAudioData() {
		// no op
		return null;
	}

	@Override
	public int getLength() {
		// no op
		return 0;
	}

	@Override
	public int getLengthWeighted() {
		// no op
		return 0;
	}

	@Override
	public int getNumberOfChannels() {
		// no op
		return 0;
	}

	@Override
	public void reload() {
		// no op

	}

	@Override
	public float getLoudness() {
		// no op
		return 0;
	}

	@Override
	public float getDynamicRange() {
		// no op
		return 0;
	}

	@Override
	public void setVolume(float lowest) {
		// no op

	}

	@Override
	public void changeVolume(double delta) {
		// no op

	}

	@Override
	public boolean isPlaying() {
		// no op
		return false;
	}

	@Override
	public String getComment() {
		// no op
		return null;
	}

	@Override
	public void setComment(String comment) {
		// no op

	}

	@Override
	public void applyStartPointOffset() {
		// no op

	}

	@Override
	public void resetStartPointOffset() {
		// no op

	}

	@Override
	public long getStartPointOffset() {
		// no op
		return 0;
	}

	@Override
	public void registerCommentChangeListener(ChangeListener<? super String> listener) {
		// no op

	}

	@Override
	public float getSampleRate() {
		// no op
		return 0;
	}

}

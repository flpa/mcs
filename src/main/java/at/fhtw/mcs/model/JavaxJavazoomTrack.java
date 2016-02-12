package at.fhtw.mcs.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.io.FilenameUtils;

import at.fhtw.mcs.util.AudioOuput;
import at.fhtw.mcs.util.FormatDetection;
import at.fhtw.mcs.util.TrackFactory.UnsupportedFormatException;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;

public class JavaxJavazoomTrack implements Track {
	private static final int BUFFER_LENGTH = 1024;
	/**
	 * Percentage used for start point calculation. Has been empirically
	 * determined.
	 */
	private static final double EXPECTED_START_POINT_PERCENTAGE = 0.8;

	private String name;
	private File file;
	private Property<String> comment = new SimpleObjectProperty<String>("");
	private Clip clip;
	private float loudness;
	private float dynamicRange;
	private float deltaVolume = 0;
	private float[] audioData;
	private int frameLength;
	private int numberOfChannels = 0;
	private int startOffsetFrames;
	private long startOffsetMicroseconds;

	/**
	 * Creates the track using the given {@link FormatDetection}.
	 * 
	 * @throws UnsupportedFormatException
	 *             In case the format of the track is not supported.
	 */
	public JavaxJavazoomTrack(FormatDetection formatDetection, String path, String projectDirectory) {
		Format format = formatDetection.detectFormat(path);
		name = FilenameUtils.getBaseName(path);
		String pathToLoad;

		switch (format) {
			case AIFF:
			case WAV:
				pathToLoad = path;
				break;
			case MP3:
				pathToLoad = convertMp3ToWav(path, projectDirectory);
				break;
			default:
				throw new UnsupportedFormatException(format);
		}
		file = new File(pathToLoad);
		this.readAudioFormatData();

		try {
			storeAudioData();
			calculateLoudness();
			calculateDynamicRange();
		} catch (UnsupportedAudioFileException | IOException e) {
			throw new RuntimeException("Unexpected error during audio analysis", e);
		}
		clip = AudioOuput.openClip(file);
	}

	public void applyStartPointOffset() {
		int startPoint = findStartPoint();
		startOffsetFrames = startPoint / 2;
		startOffsetMicroseconds = framesToMicroseconds(startOffsetFrames);
		clip.setFramePosition(startOffsetFrames);
	}

	public void resetStartPointOffset() {
		startOffsetFrames = 0;
		startOffsetMicroseconds = 0;
	}

	private int findStartPoint() {
		/*
		 * The basic idea here is that we're looking for the first rise of the
		 * level. This actual level we're looking for depends on the loudness:
		 * For louder tracks, it will be higher. That's why we effectively
		 * multiply 1 (the absolute maximum level) with the float factor of the
		 * loudness (e.g. 0.25 for -6dB). The resulting value is reduced by an
		 * empirically determined percentage so that the expected value is found
		 * earlier.
		 */
		double expextedStartValue = 1 * decibelToFloat(loudness) * EXPECTED_START_POINT_PERCENTAGE;

		int middledStartIndex = 0;
		int firstIndexAboveThreshold = -1;
		for (int i = 0; i < audioData.length; i++) {
			float value = Math.abs(audioData[i]);

			if (firstIndexAboveThreshold == -1 && value >= expextedStartValue) {
				firstIndexAboveThreshold = i;
			} else if (firstIndexAboveThreshold > -1 && value < expextedStartValue) {
				middledStartIndex = (i + firstIndexAboveThreshold) / 2;
				break;
			}
		}
		return middledStartIndex;
	}

	private String convertMp3ToWav(String path, String projectDirectory) {
		// TODO uppercase, e.g. MP3
		// TODO test: josh.new.mp3.mp3
		Converter converter = new Converter();
		File f = new File(projectDirectory, name + ".wav");
		try {
			converter.convert(path, f.getAbsolutePath());
		} catch (JavaLayerException e) {
			throw new UnsupportedFormatException(Format.MP3,
					"There was an error while converting the MP3 to WAV. Try consulting JavaZoom documentation.", e);
		}

		// throw exception if converted file does not exist
		if (!Files.exists(Paths.get(f.getAbsolutePath()))) {
			throw new UnsupportedFormatException(Format.MP3,
					"There was an error while converting the MP3 to WAV. Try consulting JavaZoom documentation.");
		}

		return f.getAbsolutePath();
	}

	private void storeAudioData() throws UnsupportedAudioFileException, IOException, UnsupportedFormatException {
		AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
		AudioFormat audioFormat = fileFormat.getFormat();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		if (audioFormat.getSampleSizeInBits() > 16) {
			System.out.println(audioFormat.getSampleSizeInBits());
			throw new UnsupportedFormatException(Format.WAV);
		}

		int nBufferSize = BUFFER_LENGTH * audioFormat.getFrameSize();
		final int normalBytes = normalBytesFromBits(audioFormat.getSampleSizeInBits());
		byte[] bytes = new byte[nBufferSize];

		AudioInputStream inputAIS = AudioSystem.getAudioInputStream(file);

		int bytesRead;

		while ((bytesRead = inputAIS.read(bytes)) != -1) {
			byteArrayOutputStream.write(bytes, 0, bytesRead);
		}

		byte[] byteAudioData = byteArrayOutputStream.toByteArray();

		float[] samples = new float[(byteAudioData.length / normalBytes)];

		unpack(byteAudioData, samples, byteAudioData.length, audioFormat);
		audioData = samples;
	}

	@Override
	public void play() {
		clip.start();
	}

	@Override
	public void pause() {
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
		clip.setFramePosition(startOffsetFrames);
	}

	/**
	 * Copied from com.sun.media.sound.Toolkit.frames2micros(AudioFormat, long)
	 */
	private long framesToMicroseconds(long frames) {
		return (long) (((double) frames) / clip.getFormat().getFrameRate() * 1000000.0d);
	}

	@Override
	public long getCurrentMicroseconds() {
		long currentMicroseconds = clip.getMicrosecondPosition() - startOffsetMicroseconds;
		/*
		 * Ensure that this method never returns a negative value, which can
		 * happend if the startOffsetMicroseconds has been calculated but the
		 * frame position has not (yet) affected the microsecondPosition.
		 */
		return currentMicroseconds < 0 ? 0 : currentMicroseconds;
	}

	@Override
	public long getTotalMicroseconds() {
		return clip.getMicrosecondLength() - startOffsetMicroseconds;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * some formats allow for bit depths in non-multiples of 8. they will,
	 * however, typically pad so the samples are stored that way. AIFF is one of
	 * these formats.
	 * 
	 * so the expression:
	 * 
	 * bitsPerSample + 7 >> 3
	 * 
	 * computes a division of 8 rounding up (for positive numbers).
	 * 
	 * this is basically equivalent to:
	 * 
	 * (int)Math.ceil(bitsPerSample / 8.0)
	 * 
	 */
	private static int normalBytesFromBits(int bitsPerSample) {
		return bitsPerSample + 7 >> 3;
	}

	public static void unpack(byte[] bytes, float[] samples, int bvalid, AudioFormat fmt) {
		if (fmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED
				&& fmt.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {
			throw new UnsupportedFormatException("Only PCM Encodings are suppported! Encoding:" + fmt.getEncoding());
		}

		long[] transfer = new long[samples.length];

		final int bitsPerSample = fmt.getSampleSizeInBits();
		final int normalBytes = normalBytesFromBits(bitsPerSample);

		// TODO: maybe combine loops (keep performance in mind!)
		if (fmt.isBigEndian()) {
			for (int i = 0, k = 0, b; i < bvalid; i += normalBytes, k++) {
				transfer[k] = 0L;

				int least = i + normalBytes - 1;
				for (b = 0; b < normalBytes; b++) {
					transfer[k] |= (bytes[least - b] & 0xffL) << (8 * b);
				}
			}
		} else {
			for (int i = 0, k = 0, b; i < bvalid; i += normalBytes, k++) {
				transfer[k] = 0L;

				for (b = 0; b < normalBytes; b++) {
					transfer[k] |= (bytes[i + b] & 0xffL) << (8 * b);
				}
			}
		}

		// fullScale is the maximum possible value
		final long fullScale = (long) Math.pow(2.0, bitsPerSample - 1);

		/*
		 * the OR is not quite enough to convert, the signage needs to be
		 * corrected.
		 */

		if (fmt.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {

			/*
			 * if the samples were signed, they must be extended to the 64-bit
			 * long.
			 * 
			 * the arithmetic right shift in Java will fill the left bits with
			 * 1's if the MSB is set.
			 * 
			 * so sign extend by first shifting left so that if the sample is
			 * supposed to be negative, it will shift the sign bit in to the
			 * 64-bit MSB then shift back and fill with 1's.
			 * 
			 * as an example, imagining these were 4-bit samples originally and
			 * the destination is 8-bit, if we have a hypothetical sample -5
			 * that ought to be negative, the left shift looks like this:
			 * 
			 * 00001011 << (8 - 4) =========== 10110000
			 * 
			 * (except the destination is 64-bit and the original bit depth from
			 * the file could be anything.)
			 * 
			 * and the right shift now fills with 1's:
			 * 
			 * 10110000 >> (8 - 4) =========== 11111011
			 */

			final long signShift = 64L - bitsPerSample;

			for (int i = 0; i < transfer.length; i++) {
				transfer[i] = ((transfer[i] << signShift) >> signShift);
			}
		} else {

			/*
			 * unsigned samples are easier since they will be read correctly in
			 * to the long.
			 * 
			 * so just sign them: subtract 2^(bits - 1) so the center is 0.
			 */

			for (int i = 0; i < transfer.length; i++) {
				transfer[i] -= fullScale;
			}
		}

		/* finally normalize to range of -1.0f to 1.0f */

		for (int i = 0; i < transfer.length; i++) {
			samples[i] = (float) transfer[i] / (float) fullScale;
		}
	}

	@Override
	public float[] getAudioData() {
		if (startOffsetFrames > 0) {
			/*
			 * TODO: temporarily, this means that there's another copy of the
			 * audiodata in memory. is that a problem? We could write a custom
			 * wrapper class ('FloatArraySlice') or maybe there's something in
			 * this thread:
			 * https://stackoverflow.com/questions/1100371/grab-a-segment-of-an-
			 * array-in-java-without-creating-a-new-array-on-heap
			 */
			return Arrays.copyOfRange(audioData, startOffsetFrames * 2, audioData.length);
		}
		return audioData;
	}

	@Override
	public int getLength() {
		return this.frameLength - this.startOffsetFrames;
	}

	@Override
	public int getLengthWeighted() {
		return (int) (getLength() * (44100 / clip.getFormat().getSampleRate()));
	}

	/**
	 * @return the dB Value of a float (-1.0 to 1.0)
	 */
	private float floatToDecibel(float sample) {
		float db;
		sample = Math.abs(sample);

		if (sample != 0.0f) {
			db = (float) (20.0f * Math.log10(sample));
		} else {
			db = -144.0f;
		}
		return db;
	}

	/**
	 * @return a float (-1.0 to 1.0) of a dB-Value
	 */
	private float decibelToFloat(float dB) {
		float sample;

		if (dB != -144.0f) {
			sample = (float) Math.pow(10, (dB / 20));
		} else {
			sample = 0.0f;
		}

		return sample;
	}

	private void calculateLoudness() {
		float loudnessFloat = 0;
		int audioFileLength = this.getLength();

		int x = 0;
		float sum = 0;
		/*
		 * RMS (Root mean square) loudness calculation
		 */
		for (int j = 0; j < audioFileLength * this.numberOfChannels; j += this.numberOfChannels) {
			float mean = 0;
			float leftChannel = audioData[j];
			if (this.numberOfChannels == 2) {
				float rightChannel = audioData[j + 1];
				mean = (leftChannel + rightChannel) / 2;

			} else {
				mean = leftChannel;
			}
			x++;
			sum += Math.pow(mean, 2);
		}
		loudnessFloat = (float) Math.sqrt(sum / x);
		loudness = floatToDecibel(loudnessFloat);
	}

	private void readAudioFormatData() {
		AudioFileFormat fileFormat;
		try {
			fileFormat = AudioSystem.getAudioFileFormat(file);
		} catch (UnsupportedAudioFileException | IOException e) {
			throw new UnsupportedFormatException(Format.WAV);
		}
		AudioFormat audioFormat = fileFormat.getFormat();
		this.numberOfChannels = audioFormat.getChannels();
		this.frameLength = fileFormat.getFrameLength();
	}

	@Override
	public int getNumberOfChannels() {
		return this.numberOfChannels;
	}

	@Override
	public void reload() {
		/*
		 * Fetch important clip data and dispose of the old clip.
		 */
		boolean wasRunning = clip.isRunning();
		clip.stop();
		int framePosition = clip.getFramePosition();
		float gain = getGainControl(clip).getValue();
		clip.close();

		/*
		 * Open a new clip with the same properties as the old one.
		 */
		Clip newClip = AudioOuput.openClip(file);
		newClip.setFramePosition(framePosition);
		getGainControl(newClip).setValue(gain);
		if (wasRunning) {
			newClip.start();
		}
		clip = newClip;
	}

	@Override
	public float getLoudness() {
		return this.loudness;
	}

	private FloatControl getGainControl(Clip clip) {
		return (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
	}

	@Override
	public void setVolume(float lowest) {
		FloatControl gainController = getGainControl(this.clip);
		float deltaDBValue = this.loudness - lowest;
		this.deltaVolume = deltaDBValue * 1.05f;

		gainController.setValue(0 - this.deltaVolume);
	}

	@Override
	public void setCurrentMicroseconds(long currentMicroseconds) {
		clip.setMicrosecondPosition(currentMicroseconds + startOffsetMicroseconds);
	}

	@Override
	public boolean isPlaying() {
		return clip.isRunning();
	}

	@Override
	public void changeVolume(double delta) {
		FloatControl gainController = getGainControl(this.clip);

		gainController.setValue(0 - this.deltaVolume);

		float temp = (float) (40f * delta);

		gainController.setValue(temp - 40 - this.deltaVolume);
	}

	/**
	 * calculates dynamic range of track (as difference between peak and rms)
	 */
	private void calculateDynamicRange() {
		int channels = this.getNumberOfChannels();
		int audioFileLength = this.getLength();

		float peak = 0;
		float meanPrevious = 0;
		float meanCurrent = 0;
		float meanNext = 0;

		for (int j = 0; j < audioFileLength * channels; j += channels) {
			// first sample
			if (j == 0) {
				float leftChannel = audioData[j];
				if (channels == 2) {
					float rightChannel = audioData[j + 1];
					meanCurrent = Math.abs((leftChannel + rightChannel) / 2);

				} else {
					meanCurrent = Math.abs(leftChannel);
				}
			}

			// next sample
			if (j + channels < audioFileLength * channels) {
				float leftChannel = audioData[j + channels];
				if (channels == 2) {
					float rightChannel = audioData[j + channels + 1];
					meanNext = Math.abs((leftChannel + rightChannel) / 2);

				} else {
					meanNext = Math.abs(leftChannel);
				}
			} else {
				meanNext = 0;
			}

			// check if sample is peak
			if (meanPrevious < meanCurrent && meanCurrent > meanNext && meanCurrent > peak) {
				peak = meanCurrent;
			}

			// set current as previous and next as current sample
			meanPrevious = meanCurrent;
			meanCurrent = meanNext;

		}

		dynamicRange = this.getLoudness() - this.floatToDecibel(peak);
	}

	@Override
	public void registerCommentChangeListener(ChangeListener<? super String> listener) {
		this.comment.addListener(listener);
	}

	@Override
	public float getDynamicRange() {
		return this.dynamicRange;
	}

	@Override
	public String getComment() {
		return comment.getValue();
	}

	@Override
	public void setComment(String comment) {
		this.comment.setValue(comment);
	}

	@Override
	public String getPath() {
		return file.getAbsolutePath();
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getFileName() {
		return String.format("%s.%s", name, FilenameUtils.getExtension(file.getPath()));
	}

	public void saveAs(File destination) throws IOException {
		Files.copy(file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
		file = destination;
	}

	@Override
	public long getStartPointOffset() {
		return startOffsetMicroseconds;
	}

	@Override
	public float getSampleRate() {
		return clip.getFormat().getSampleRate();
	}

	@Override
	public Property<String> commentProperty() {
		return comment;
	}
}

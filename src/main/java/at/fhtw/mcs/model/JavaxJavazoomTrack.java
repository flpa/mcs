package at.fhtw.mcs.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.BooleanControl.Type;
import javax.sound.sampled.Clip;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.io.FilenameUtils;

import at.fhtw.mcs.util.AudioOuput;
import at.fhtw.mcs.util.FormatDetection;
import at.fhtw.mcs.util.TrackFactory.UnsupportedFormatException;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;

public class JavaxJavazoomTrack implements Track {
	private static final int BUFFER_LENGTH = 1024;

	private String path;
	private Clip clip;
	private float loudness;
	private Vector<float[]> audioData = new Vector<float[]>();
	private int numberOfChannels = 0;

	/**
	 * Creates the track using the given {@link FormatDetection}.
	 * 
	 * @throws UnsupportedFormatException
	 *             In case the format of the track is not supported.
	 */
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

		clip = AudioOuput.openClip(new File(this.path));

		numberOfChannels = this.setNumberOfChannels();

		try {
			storeAudioData(this.path);
			calculateLoudness();
		} catch (UnsupportedAudioFileException | IOException e) {
			throw new RuntimeException("Unexpected error during audio analysis", e);
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
					"There was an error while converting the MP3 to WAV. Try consulting JavaZoom documentation.", e);
		}

		// throw exception if converted file does not exist
		if (!Files.exists(Paths.get(newPath))) {
			throw new UnsupportedFormatException(Format.MP3,
					"There was an error while converting the MP3 to WAV. Try consulting JavaZoom documentation.");
		}

		return newPath;
	}

	private void storeAudioData(String path) throws UnsupportedAudioFileException, IOException {
		File sourceFile = new File(path);
		AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(sourceFile);
		AudioFormat audioFormat = fileFormat.getFormat();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		int nBufferSize = BUFFER_LENGTH * audioFormat.getFrameSize();
		final int normalBytes = normalBytesFromBits(audioFormat.getSampleSizeInBits());
		byte[] bytes = new byte[nBufferSize];

		AudioInputStream inputAIS = AudioSystem.getAudioInputStream(sourceFile);

		int bytesRead;

		while ((bytesRead = inputAIS.read(bytes)) != -1) {
			byteArrayOutputStream.write(bytes, 0, bytesRead);
		}

		byte[] byteAudioData = byteArrayOutputStream.toByteArray();

		float[] samples = new float[(byteAudioData.length / normalBytes) * audioFormat.getChannels()];

		unpack(byteAudioData, samples, byteAudioData.length, audioFormat);
		audioData.add(samples);
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
		clip.setFramePosition(0);
	}

	@Override
	public long getCurrentMicroseconds() {
		return clip.getMicrosecondPosition();
	}

	@Override
	public long getTotalMicroseconds() {
		return clip.getMicrosecondLength();
	}

	@Override
	public String getFilename() {
		return FilenameUtils.getName(path);
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
		 * 
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
			 * 
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
			 * 
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
	public Vector<float[]> getAudioData() {
		return audioData;
	}

	@Override
	public int getLength() {
		return clip.getFrameLength();
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

	@Override
	public void calculateLoudness() {
		float loudnessFloat = 0;
		AudioFormat audioFormat = clip.getFormat();
		int channels = audioFormat.getChannels();
		int audioFileLength = this.getLength();

		int x = 0;
		float sum = 0;
		/*
		 * RMS (Root mean square) loudness calculation
		 */
		for (int i = 0; i < audioData.size(); i++) {

			if (this.audioData.elementAt(i) == null) {
				break;
			}

			for (int j = 0; j < audioFileLength * channels; j++) {
				float mean = 0;
				float leftChannel = audioData.elementAt(i)[j];
				if (channels == 2) {
					float rightChannel = audioData.elementAt(i)[j + 1];
					mean = (leftChannel + rightChannel) / 2;

				} else {
					mean = leftChannel;
				}
				x++;
				sum += Math.pow(mean, 2);
			}
		}
		loudnessFloat = (float) Math.sqrt(sum / x);
		loudness = floatToDecibel(loudnessFloat);
		System.out.println(loudness);
	}

	private int setNumberOfChannels() {
		File sourceFile = new File(this.path);
		AudioFileFormat fileFormat;
		try {
			fileFormat = AudioSystem.getAudioFileFormat(sourceFile);
		} catch (UnsupportedAudioFileException | IOException e) {
			throw new UnsupportedFormatException(Format.MP3,
					"There was an error while converting the MP3 to WAV. Try consulting JavaZoom documentation.", e);
		}
		AudioFormat audioFormat = fileFormat.getFormat();
		return audioFormat.getChannels();
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
		boolean wasMuted = getMuteControl(clip).getValue();
		clip.stop();
		int framePosition = clip.getFramePosition();
		clip.close();

		/*
		 * Open a new clip with the same properties as the old one.
		 */
		Clip newClip = AudioOuput.openClip(new File(path));
		newClip.setFramePosition(framePosition);
		getMuteControl(newClip).setValue(wasMuted);
		if (wasRunning) {
			newClip.start();
		}
		clip = newClip;
	}

	@Override
	public void mute() {
		getMuteControl(clip).setValue(true);
	}

	private BooleanControl getMuteControl(Clip c) {
		return (BooleanControl) c.getControl(Type.MUTE);
	}

	@Override
	public void unmnute() {
		getMuteControl(clip).setValue(false);
	}
}

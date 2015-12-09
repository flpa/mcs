package at.fhtw.mcs.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.io.FilenameUtils;

import at.fhtw.mcs.model.TrackFactory.UnsupportedFormatException;
import at.fhtw.mcs.util.FormatDetection;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;

public class JavaxJavazoomTrack implements Track {
	private String path;
	private Clip clip;
	private int framePosition = 0;
	private int frameLength;
	private float loudness;
	private Vector<float[]> audioData = new Vector<float[]>();

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

		clip = openClip();

		frameLength = clip.getFrameLength();

		// calls a function which calculates als the amplitude Data as floats
		// and a function that calculates the loudness
		try {
			storeData(this.path);
			calculateLoudness();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

		return newPath;
	}

	private Clip openClip() throws RuntimeException {
		try {
			URL url = new File(path).toURI().toURL();
			AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
			Clip clip = AudioSystem.getClip();
			clip.open(audioIn);
			return clip;
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			throw new RuntimeException("Error while opening clip", e);
		}
	}

	private void storeData(String path) throws UnsupportedAudioFileException, IOException {

		final int BUFFER_LENGTH = 1024;

		File sourceFile = new File(path);
		AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(sourceFile);
		AudioFormat audioFormat = fileFormat.getFormat();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		int nBufferSize = BUFFER_LENGTH * audioFormat.getFrameSize();
		final int normalBytes = normalBytesFromBits(audioFormat.getSampleSizeInBits());
		byte[] bytes = new byte[nBufferSize];

		AudioInputStream inputAIS = AudioSystem.getAudioInputStream(sourceFile);

		int bytesRead;

		while (true) {
			bytesRead = inputAIS.read(bytes);
			// bread = inputAIS.read(bytes);

			if (bytesRead == -1) {
				break;
			}

			byteArrayOutputStream.write(bytes, 0, bytesRead);
		}

		byte[] byteAudioData = byteArrayOutputStream.toByteArray();

		float[] samples = new float[(byteAudioData.length / normalBytes) * audioFormat.getChannels()];
		long[] transfer = new long[samples.length];

		samples = unpack(byteAudioData, transfer, samples, byteAudioData.length, audioFormat);
		audioData.add(samples);

	}

	@Override
	public void play() {
		clip.setFramePosition(framePosition);
		clip.start();
	}

	@Override
	public void pause() {
		framePosition = clip.getFramePosition();
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
		framePosition = 0;
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

	public static int normalBytesFromBits(int bitsPerSample) {

		/*
		 * some formats allow for bit depths in non-multiples of 8. they will,
		 * however, typically pad so the samples are stored that way. AIFF is
		 * one of these formats.
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

		return bitsPerSample + 7 >> 3;
	}

	public static float[] unpack(byte[] bytes, long[] transfer, float[] samples, int bvalid, AudioFormat fmt) {
		if (fmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED
				&& fmt.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {

			return samples;
		}

		final int bitsPerSample = fmt.getSampleSizeInBits();
		final int normalBytes = normalBytesFromBits(bitsPerSample);

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

		return samples;
	}

	@Override
	public Vector<float[]> getAudioData() {
		return audioData;
	}

	@Override
	public int getLength() {
		return frameLength;
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
		for (int i = 0; i < audioData.size(); i++) {

			if (this.audioData.elementAt(i) == null) {
				break;
			}

			for (int j = 0; j < audioFileLength * channels; j++) {

				float leftChannel = audioData.elementAt(i)[j];
				float rightChannel = audioData.elementAt(i)[j + 1];
				float mean = (leftChannel + rightChannel) / 2;
				x++;
				sum += Math.pow(mean, 2);
			}
		}
		loudnessFloat = (float) Math.sqrt(sum / x);
		loudness = floatToDecibel(loudnessFloat);
		System.out.println(loudness);

	}
}

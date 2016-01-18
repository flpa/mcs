package at.fhtw.mcs.util;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import at.fhtw.mcs.model.Format;

public class MimeTypeFormatDetectionTest {

	FormatDetection impl = new MimeTypeFormatDetection();

	@Test
	public void mp3() throws Exception {
		testFormat("piano4824.mp3", Format.MP3);
	}

	@Test
	public void aiff() throws Exception {
		testFormat("piano4416.aif", Format.AIFF);
	}

	@Test
	public void wav() throws Exception {
		testFormat("piano4824.wav", Format.WAV);
	}

	@Test
	public void unknown() throws Exception {
		testFormat("not-an-audio-file.txt", Format.UNKNOWN);
	}

	private void testFormat(String fileName, Format expectedFormat) {
		Format format = impl.detectFormat(new File(fileName).getAbsolutePath());
		Assert.assertEquals(expectedFormat, format);
	}

}

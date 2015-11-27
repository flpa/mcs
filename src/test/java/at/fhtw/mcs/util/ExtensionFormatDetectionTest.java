package at.fhtw.mcs.util;

import org.junit.Assert;
import org.junit.Test;

import at.fhtw.mcs.model.Format;

public class ExtensionFormatDetectionTest {

	ExtensionFormatDetection impl = new ExtensionFormatDetection();

	@Test
	public void testWavLowercase() {
		Assert.assertEquals(Format.WAV, impl.detectFormat("/home/joe/rock.wav"));
	}

	@Test
	public void testWavUppercase() {
		Assert.assertEquals(Format.WAV, impl.detectFormat("/home/joe/rock.WAV"));
	}

	@Test
	public void testWavMixedcase() {
		Assert.assertEquals(Format.WAV, impl.detectFormat("/home/joe/rock.Wav"));
	}

	@Test
	public void testAiffLowercase() {
		Assert.assertEquals(Format.AIFF, impl.detectFormat("/home/joe/rock.aiff"));
	}

	@Test
	public void testAiffUppercase() {
		Assert.assertEquals(Format.AIFF, impl.detectFormat("/home/joe/rock.AIFF"));
	}

	@Test
	public void testAiffMixedcase() {
		Assert.assertEquals(Format.AIFF, impl.detectFormat("/home/joe/rock.AIff"));
	}

	@Test
	public void testAifLowercase() {
		Assert.assertEquals(Format.AIFF, impl.detectFormat("/home/joe/rock.aif"));
	}

	@Test
	public void testAifUppercase() {
		Assert.assertEquals(Format.AIFF, impl.detectFormat("/home/joe/rock.AIF"));
	}

	@Test
	public void testAifMixedcase() {
		Assert.assertEquals(Format.AIFF, impl.detectFormat("/home/joe/rock.Aif"));
	}

	@Test
	public void testMp3Lowercase() {
		Assert.assertEquals(Format.MP3, impl.detectFormat("/home/joe/rock.mp3"));
	}

	@Test
	public void testMp3Uppercase() {
		Assert.assertEquals(Format.MP3, impl.detectFormat("/home/joe/rock.MP3"));
	}

	@Test
	public void testMp3Mixedcase() {
		Assert.assertEquals(Format.MP3, impl.detectFormat("/home/joe/rock.Mp3"));
	}

	@Test
	public void testMp3WithWavInName() {
		Assert.assertEquals(Format.MP3, impl.detectFormat("/home/joe/rock.wav.mp3"));
	}
}

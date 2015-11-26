package at.fhtw.mcs.util;

import org.junit.Assert;
import org.junit.Test;

import at.fhtw.mcs.model.Format;

public class ExtensionFormatDetectionTest {

	ExtensionFormatDetection impl = new ExtensionFormatDetection();

	@Test
	public void testWavLowercase() {
		Assert.assertEquals(Format.MP3, impl.detectFormat("/home/joe/rock.wav"));
	}

}

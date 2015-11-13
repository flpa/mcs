package at.fhtw.mcs;

import org.junit.Assert;
import org.junit.Test;

public class MainTest {

	@Test
	public void testTestedMethod()  {
		Main m = new Main();
		Assert.assertEquals(4, m.testedMethod(2));
	}
}

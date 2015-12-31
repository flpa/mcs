package at.fhtw.mcs.util;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Utility class like {@link Arrays}.
 */
public class Arrays2 {

	public static DoubleStream stream(float[] floats) {
		return IntStream.range(0, floats.length).mapToDouble(i -> floats[i]);
	}
}

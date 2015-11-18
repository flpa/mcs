package at.fhtw.mcs;

import java.util.Arrays;
import java.util.stream.Collectors;

class Main {

	public static void main(String[] args) {
		/*
		 * Don't try this at home kids, just using a random Java 8 feature to
		 * ensure Gradle runs with J8.
		 */

		//@formatter:off
		System.out.println("Hallo " + 
				Arrays.stream(new String[] { "Josh", "Ralf" })
					  .map(name -> "'" + name + "'")
					  .collect(Collectors.joining(" und ")) 
				+ ".");
		//@formatter:on
	}

	public int testedMethod(int n) {
		return n * 2;
	}
}

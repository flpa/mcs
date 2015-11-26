package at.fhtw.mcs;

import java.util.Arrays;
import java.util.stream.Collectors;

import at.fhtw.mcs.model.Track;
import at.fhtw.mcs.model.TrackFactory;

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

		Track loadTrack = TrackFactory.loadTrack("");
	}

	public int testedMethod(int n) {
		return n * 2;
	}
}

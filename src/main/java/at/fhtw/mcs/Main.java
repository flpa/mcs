package at.fhtw.mcs;

import java.util.Scanner;

import at.fhtw.mcs.model.Track;
import at.fhtw.mcs.model.TrackFactory;

class Main {

	public static void main(String[] args) throws InterruptedException {
		String filename = args[0];
		System.out.println("Loading " + filename);
		Track track = TrackFactory.loadTrack(filename);
		track.play();

		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				switch (scanner.nextLine()) {
					case "p":
						track.togglePlayPause();
						break;
					case "s":
						track.stop();
						break;
					case "q":
						return;
				}
			}
		}
	}
}

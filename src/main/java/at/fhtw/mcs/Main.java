package at.fhtw.mcs;

import at.fhtw.mcs.model.TrackFactory;

class Main {

	public static void main(String[] args) throws InterruptedException {
		String filename = args[0];
		System.out.println("Loading " + filename);

		TrackFactory.loadTrack(filename).play();
		Thread.sleep(5000);
	}
}

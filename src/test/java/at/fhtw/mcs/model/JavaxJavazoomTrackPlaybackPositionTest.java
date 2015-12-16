package at.fhtw.mcs.model;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

/**
 * Integration test for testing track playback position behaviour. Does actually
 * open and play the track; so probably its not extremely robust. It proves that
 * there are issues (at least on my system), though.
 * 
 * @author florian.patzl@technikum-wien.at
 *
 */
public class JavaxJavazoomTrackPlaybackPositionTest {

	/**
	 * On my machine, this stops the track at 22 microseconds.
	 */
	@Test
	public void testStopResetsCurrentMicroseconds() throws InterruptedException {
		JavaxJavazoomTrack track = openTestTrack();

		track.play();
		Thread.sleep(1000);
		track.stop();

		Assert.assertEquals(0, track.getCurrentMicroseconds());
	}

	/**
	 * On my machine, this pauses the track at 1s(=1000ms)
	 */
	@Test
	public void testPauseStopsAtCurrentLocation() throws InterruptedException {
		long sleepTimeMs = 10;
		JavaxJavazoomTrack track = openTestTrack();

		track.play();
		Thread.sleep(sleepTimeMs);
		track.pause();

		long currentMilliseconds = TimeUnit.MICROSECONDS.toMillis(track.getCurrentMicroseconds());
		Assert.assertThat(currentMilliseconds,
				is(both(greaterThanOrEqualTo(sleepTimeMs)).and(lessThanOrEqualTo(sleepTimeMs * 2))));
	}

	private JavaxJavazoomTrack openTestTrack() {
		URL resource = getClass().getClassLoader().getResource("piano4416.wav");
		JavaxJavazoomTrack track = new JavaxJavazoomTrack(s -> Format.WAV, resource.getPath());
		return track;
	}
}

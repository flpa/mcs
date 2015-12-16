package at.fhtw.mcs.util;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * This class centralizes management of audio output. It maintains the selected
 * {@link Mixer} and is reponsible for opening {@link Clip}s.
 *
 */
public class AudioOuput {

	private AudioOuput() {
		// no instances allowed
	}

	private static Mixer.Info selectedMixerInfo = AudioSystem.getMixerInfo()[0];

	public static Clip openClip(File file) {
		System.out.println("Opening " + file.getAbsolutePath());
		try {
			AudioInputStream audioIn = AudioSystem.getAudioInputStream(file);
			Clip clip = AudioSystem.getClip(selectedMixerInfo);
			clip.open(audioIn);
			return clip;
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			throw new RuntimeException("Error while opening clip", e);
		}
	}

	public static Mixer.Info getSelectedMixerInfo() {
		return selectedMixerInfo;
	}

	public static void setSelectedMixerInfo(Mixer.Info selectedMixerInfo) {
		AudioOuput.selectedMixerInfo = selectedMixerInfo;
	}
}

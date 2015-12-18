package at.fhtw.mcs.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

import at.fhtw.mcs.model.Format;
import at.fhtw.mcs.model.Track;
import at.fhtw.mcs.util.AudioOuput;
import at.fhtw.mcs.util.TrackFactory;
import at.fhtw.mcs.util.TrackFactory.UnsupportedFormatException;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Controller class for Root.fxml
 */
public class RootController implements Initializable {

	/*
	 * Where to move those? ResourceBundles?
	 */
	private static final String ICON_PAUSE = "||";
	private static final String ICON_PLAY = "▶";

	@FXML
	private VBox vboxTracks;
	@FXML
	private Menu menuOutputDevices;
	@FXML
	private MenuItem menuItemQuit;
	@FXML
	private MenuItem menuItemAddTrack;
	@FXML
	private MenuItem menuItemAbout;
	@FXML
	private Button buttonPlayPause;
	@FXML
	private Button buttonStop;
	@FXML
	private Button buttonAddTrack;
	@FXML
	private Text textCurrentTime;
	@FXML
	private Text textTotalTime;
	@FXML
	private ProgressBar progressBarTime;

	private ResourceBundle bundle;
	private Stage stage;

	private List<Track> tracks = new ArrayList<>();

	// TODO: config parameter
	private long updateFrequencyMs = 100;

	public RootController(Stage stage) {
		this.stage = stage;
	}

	@Override
	public void initialize(URL viewSource, ResourceBundle translations) {
		this.bundle = translations;

		// 'x -> functionCall' is a minimalistic Java8 lambda
		menuItemQuit.setOnAction(e -> Platform.exit());
		menuItemAddTrack.setOnAction(this::handleAddTrack);
		menuItemAbout.setOnAction(this::handleAbout);

		// TODO: inline lambdas vs methods?
		buttonPlayPause.setOnAction(e -> {
			tracks.forEach(Track::togglePlayPause);
			buttonPlayPause.setText(ICON_PLAY.equals(buttonPlayPause.getText()) ? ICON_PAUSE : ICON_PLAY);
		});
		buttonStop.setOnAction(e -> {
			tracks.forEach(Track::stop);
			buttonPlayPause.setText(ICON_PLAY);
		});
		buttonAddTrack.setOnAction(this::handleAddTrack);

		ToggleGroup group = new ToggleGroup();

		//@formatter:off
		Arrays.stream(AudioSystem.getMixerInfo())
				.filter(RootController::isOutputMixerInfo)
				.forEach(info -> {
					RadioMenuItem radio = new RadioMenuItem();
					radio.setText(String.format("%s (%s)", info.getName(), info.getDescription()));
					radio.setUserData(info);
					radio.setToggleGroup(group);
					radio.setSelected(info.equals(AudioOuput.getSelectedMixerInfo()));
					menuOutputDevices.getItems().add(radio);
		});
		//@formatter:on

		group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			public void changed(ObservableValue<? extends Toggle> value, Toggle previousSelection,
					Toggle newSelection) {
				/*
				 * When modifying grouped RadioMenuItems, this is invoked twice:
				 * 1) oldValue and null 2) null and newValue
				 */
				if (newSelection != null) {
					AudioOuput.setSelectedMixerInfo((Mixer.Info) newSelection.getUserData());
					tracks.forEach(Track::reload);
				}
			}
		});
	}

	private static boolean isOutputMixerInfo(Mixer.Info info) {
		return AudioSystem.getMixer(info).isLineSupported(new Line.Info(Clip.class));
	}

	private void updateTime() {
		if (tracks.isEmpty()) {
			return;
		}
		// TODO: for now, we'll assume only checking the first track is ok.
		Track track = tracks.get(0);
		long currentMicroseconds = track.getCurrentMicroseconds();
		long totalMicroseconds = track.getTotalMicroseconds();
		progressBarTime.setProgress((double) currentMicroseconds / totalMicroseconds);
		textCurrentTime.setText(formatTimeString(currentMicroseconds));

		// TODO: reset playPause button; doesn't work
		// if (currentMicroseconds == totalMicroseconds) {
		// buttonPlayPause.setText(ICON_PLAY);
		// }
	}

	private void handleAddTrack(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		/*
		 * TODO: should restrict file types! but maybe don't hardcode, rather
		 * 'ask' a responsible class what file types are allowed?
		 */

		chooser.setTitle("TRANSLATE ME");
		File file = chooser.showOpenDialog(stage);
		if (file == null) {
			return;
		}

		Track track;
		try {
			track = TrackFactory.loadTrack(file.getAbsolutePath());
		} catch (UnsupportedFormatException e) {
			this.showErrorUnsupportedFormat(e.getFormat(), e.getAudioFormat());
			return;
		}

		// Things to be done for first track
		if (tracks.isEmpty()) {
			long totalMicroseconds = track.getTotalMicroseconds();
			String timeString = formatTimeString(totalMicroseconds);
			textTotalTime.setText(timeString);

			startTimeUpdateThread();
		}

		loadTrackUi(track);
		buttonPlayPause.setDisable(false);
		buttonStop.setDisable(false);

		tracks.add(track);
	}

	private void loadTrackUi(Track track) {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setController(new TrackController(track));
			loader.setLocation(getClass().getClassLoader().getResource("views/Track.fxml"));
			loader.setResources(bundle);
			vboxTracks.getChildren().add(loader.load());
		} catch (IOException e) {
			// TODO: better exception handling
			e.printStackTrace();
		}
	}

	private void startTimeUpdateThread() {
		Timer timer = new Timer(true);
		/*
		 * Reading the documentation of timer.schedule(...), it seems like
		 * there's no danger of timer-execution-congestion when a time
		 * invocation blocks: "[...]each execution is scheduled relative to the
		 * actual execution time of the previous execution."
		 */
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				long prevMillis = System.currentTimeMillis();
				updateTime();
				long elapsedMs = System.currentTimeMillis() - prevMillis;

				if (elapsedMs >= updateFrequencyMs) {
					System.err.println(
							String.format("Warning: Time update (%dms) took longer than the update frequency (%dms).",
									elapsedMs, updateFrequencyMs));
				}
			}
		}, 0, updateFrequencyMs);
	}

	private String formatTimeString(long totalMicroseconds) {
		long minutes = TimeUnit.MICROSECONDS.toMinutes(totalMicroseconds);
		long seconds = TimeUnit.MICROSECONDS.toSeconds(totalMicroseconds) % 60;

		return String.format("%d:%02d", minutes, seconds);
	}

	private void handleAbout(ActionEvent event) {
		Alert alertAbout = new Alert(AlertType.INFORMATION);
		alertAbout.setTitle(bundle.getString("about.title"));
		alertAbout.setHeaderText(null);
		alertAbout.setContentText(bundle.getString("about.contentText"));

		((Label) alertAbout.getDialogPane().getChildren().get(1)).setWrapText(false);
		alertAbout.getDialogPane().setPrefHeight(Region.USE_COMPUTED_SIZE);
		// TODO: auto-resize to content
		alertAbout.getDialogPane().setPrefWidth(700);

		alertAbout.showAndWait();
	}

	private void showErrorUnsupportedFormat(Format format, AudioFormat audioFormat) {
		Alert alertError = new Alert(AlertType.ERROR);
		alertError.setTitle(bundle.getString("errorUnsupportedFormat.title"));
		alertError.setHeaderText(null);

		String errorText = bundle.getString(determineErrorDescriptionForFormat(format, audioFormat));
		errorText += bundle.getString("errorUnsupportedFormat.supportedFormats");
		alertError.setContentText(errorText);

		alertError.getDialogPane().setPrefHeight(Region.USE_COMPUTED_SIZE);
		alertError.getDialogPane().setPrefWidth(700);

		alertError.showAndWait();
	}

	private String determineErrorDescriptionForFormat(Format format, AudioFormat audioFormat) {
		switch (format) {
			case AIFF:
			case WAV:
				if (audioFormat.getSampleSizeInBits() == 24) {
					return "errorUnsupportedFormat.content24bit";
				} else {
					return "errorUnsupportedFormat.contentDefault";
				}
			case MP3:
				return "errorUnsupportedFormat.contentMp3";
			default:
				return "errorUnsupportedFormat.contentDefault";
		}
	}
}

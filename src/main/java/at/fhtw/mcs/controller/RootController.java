package at.fhtw.mcs.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

import at.fhtw.mcs.model.Track;
import at.fhtw.mcs.util.AudioOuput;
import at.fhtw.mcs.util.TrackFactory;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Controller class for Root.fxml
 */
public class RootController implements Initializable {

	@FXML
	private VBox vboxTracks;
	@FXML
	private Menu menuOutputDevices;
	@FXML
	private MenuItem menuItemQuit;
	@FXML
	private MenuItem menuItemAddTrack;
	@FXML
	private MenuItem menuItemOutputDevices;
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

	private Track track;

	public RootController(Stage stage) {
		this.stage = stage;
	}

	@Override
	public void initialize(URL viewSource, ResourceBundle translations) {
		this.bundle = translations;

		// 'x -> functionCall' is a minimalistic Java8 lambda
		menuItemQuit.setOnAction(e -> Platform.exit());
		menuItemAddTrack.setOnAction(this::handleAddTrack);

		// TODO: inline lambdas vs methods?
		buttonPlayPause.setOnAction(e -> {
			track.togglePlayPause();
			// TODO: extract unicode constants
			buttonPlayPause.setText("▶".equals(buttonPlayPause.getText()) ? "||" : "▶");
		});
		buttonStop.setOnAction(e -> {
			track.stop();
			buttonPlayPause.setText("▶");
		});
		buttonAddTrack.setOnAction(this::handleAddTrack);

		ToggleGroup group = new ToggleGroup();

		// TODO: mistery of disappearing mixer?
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
					if (track != null) {
						track.reload();
					}
				}
			}
		});
	}

	private static boolean isOutputMixerInfo(Mixer.Info info) {
		return AudioSystem.getMixer(info).isLineSupported(new Line.Info(Clip.class));
	}

	private void updateTime() {
		progressBarTime.setProgress((double) track.getCurrentMicroseconds() / track.getTotalMicroseconds());
		textCurrentTime.setText(formatTimeString(track.getCurrentMicroseconds()));
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

		track = TrackFactory.loadTrack(file.getAbsolutePath());
		long totalMicroseconds = track.getTotalMicroseconds();
		String timeString = formatTimeString(totalMicroseconds);
		textTotalTime.setText(timeString);

		// TODO: config parameter
		long updateFrequencyMs = 100;

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

		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setController(new TrackController(track));
			loader.setLocation(getClass().getClassLoader().getResource("views/Track.fxml"));
			loader.setResources(bundle);
			Node track = loader.load();
			vboxTracks.getChildren().add(track);
		} catch (IOException e) {
			e.printStackTrace();
		}
		buttonPlayPause.setDisable(false);
		buttonStop.setDisable(false);
	}

	private String formatTimeString(long totalMicroseconds) {
		long minutes = TimeUnit.MICROSECONDS.toMinutes(totalMicroseconds);
		long seconds = TimeUnit.MICROSECONDS.toSeconds(totalMicroseconds) % 60;

		return String.format("%d:%02d", minutes, seconds);
	}
}

package at.fhtw.mcs.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import at.fhtw.mcs.Main;
import at.fhtw.mcs.model.Track;
import at.fhtw.mcs.model.TrackFactory;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
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
	private MenuItem menuItemQuit;
	@FXML
	private MenuItem menuItemAddTrack;
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
	public void initialize(URL arg0, ResourceBundle arg1) {
		this.bundle = arg1;

		// Java8 lambda
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
	}

	private void updateTime() {
		progressBarTime.setProgress((double) track.getCurrentMicroseconds() / track.getTotalMicroseconds());
		textCurrentTime.setText(formatTimeString(track.getCurrentMicroseconds()));
	}

	private void handleAddTrack(ActionEvent event) {
		FileChooser chooser = new FileChooser();

		chooser.setTitle("Track wählen");
		File file = chooser.showOpenDialog(stage);

		track = TrackFactory.loadTrack(file.getAbsolutePath());
		long totalMicroseconds = track.getTotalMicroseconds();
		String timeString = formatTimeString(totalMicroseconds);
		textTotalTime.setText(timeString);

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				updateTime();
			}
			// TODO: config parameter
		}, 0, 500);

		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setController(new TrackController(track));
			loader.setLocation(Main.class.getResource("../../../views/Track.fxml"));
			Node track = loader.load();
			vboxTracks.getChildren().add(track);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String formatTimeString(long totalMicroseconds) {
		long minutes = TimeUnit.MICROSECONDS.toMinutes(totalMicroseconds);
		long seconds = TimeUnit.MICROSECONDS.toSeconds(totalMicroseconds) % 60;

		return String.format("%d:%02d", minutes, seconds);
	}
}

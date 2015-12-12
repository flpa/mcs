package at.fhtw.mcs.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import at.fhtw.mcs.model.Track;
import at.fhtw.mcs.model.TrackFactory;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
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

	private Track track;

	public RootController(Stage stage) {
		this.stage = stage;
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		this.bundle = arg1;

		// 'x -> functionCall' is a minimalistic Java8 lambda
		menuItemQuit.setOnAction(e -> Platform.exit());
		menuItemAddTrack.setOnAction(this::handleAddTrack);
		menuItemAbout.setOnAction(this::handleAbout);

		// TODO: inline lambdas vs methods?
		buttonPlayPause.setOnAction(e -> {
			track.togglePlayPause();
			buttonPlayPause.setText(ICON_PLAY.equals(buttonPlayPause.getText()) ? ICON_PAUSE : ICON_PLAY);
		});
		buttonStop.setOnAction(e -> {
			track.stop();
			buttonPlayPause.setText(ICON_PLAY);
		});
		buttonAddTrack.setOnAction(this::handleAddTrack);
	}

	private void updateTime() {
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

		track = TrackFactory.loadTrack(file.getAbsolutePath());
		long totalMicroseconds = track.getTotalMicroseconds();
		String timeString = formatTimeString(totalMicroseconds);
		textTotalTime.setText(timeString);

		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				updateTime();
			}
			// TODO: config parameter
		}, 0, 50);

		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setController(new TrackController(track));
			/*
			 * TODO: proper way of loading without depending on package
			 * structure?
			 */
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
}

package at.fhtw.mcs.controller;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import at.fhtw.mcs.model.Track;
import at.fhtw.mcs.model.TrackFactory;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
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
	private ResourceBundle bundle;
	private Stage stage;

	private Track track;

	public RootController(Stage stage) {
		this.stage = stage;
	}

	public void addTrack(Node track) {
		vboxTracks.getChildren().add(track);
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		this.bundle = arg1;

		// Java8 lambda
		menuItemQuit.setOnAction(e -> Platform.exit());
		menuItemAddTrack.setOnAction(e -> handleAddTrack(e));
		buttonPlayPause.setOnAction(e -> {
			track.togglePlayPause();
			buttonPlayPause.setText("▶".equals(buttonPlayPause.getText()) ? "||" : "▶");
		});
		buttonStop.setOnAction(e -> {
			track.stop();
			buttonPlayPause.setText("▶");
		});
	}

	private void handleAddTrack(ActionEvent e) {
		FileChooser chooser = new FileChooser();

		chooser.setTitle("Track wählen");
		File file = chooser.showOpenDialog(stage);

		track = TrackFactory.loadTrack(file.getAbsolutePath());
	}
}

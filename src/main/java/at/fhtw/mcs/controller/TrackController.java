package at.fhtw.mcs.controller;

import java.net.URL;
import java.util.ResourceBundle;

import at.fhtw.mcs.model.Track;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.text.Text;

/**
 * Controller class for Track.fxml
 */
public class TrackController implements Initializable {
	@FXML
	private Text textTrackName;

	private Track track;

	public TrackController(Track track) {
		this.track = track;

	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		textTrackName.setText(track.getFilename());
	}
}

package at.fhtw.mcs.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * Controller class for Root.fxml
 */
public class RootController {

	@FXML
	VBox vboxTracks;

	public void addTrack(Node track) {
		vboxTracks.getChildren().add(track);
	}

}

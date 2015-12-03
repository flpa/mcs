package at.fhtw.mcs.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;

/**
 * Controller class for Root.fxml
 */
public class RootController implements Initializable {

	@FXML
	private VBox vboxTracks;

	@FXML
	private MenuItem menuItemQuit;

	private ResourceBundle bundle;

	public void addTrack(Node track) {
		vboxTracks.getChildren().add(track);
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		this.bundle = arg1;

		// Java8 lambda
		menuItemQuit.setOnAction(e -> Platform.exit());
	}

}

package at.fhtw.mcs;

import java.io.IOException;

import at.fhtw.mcs.controller.RootController;
import at.fhtw.mcs.controller.TrackController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

	private Stage primaryStage;
	private BorderPane rootLayout;
	private RootController rootController;

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		this.primaryStage.setTitle("MCS");

		initRootLayout();
	}

	/**
	 * Initialize Root Layout
	 */
	public void initRootLayout() {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setController(new RootController(primaryStage));
			loader.setLocation(Main.class.getResource("../../../views/Root.fxml"));
			rootLayout = (BorderPane) loader.load();
			rootController = (RootController) loader.getController();

			Scene scene = new Scene(rootLayout);
			primaryStage.setScene(scene);
			primaryStage.setMinWidth(800);
			primaryStage.setMinHeight(550);
			primaryStage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
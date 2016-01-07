package at.fhtw.mcs;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import at.fhtw.mcs.controller.RootController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

	private Stage primaryStage;
	private BorderPane rootLayout;

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		initRootLayout();
	}

	/**
	 * Initialize Root Layout
	 */
	public void initRootLayout() {
		try {
			FXMLLoader loader = new FXMLLoader();
			RootController rootController = new RootController(primaryStage);
			loader.setController(rootController);
			loader.setLocation(getClass().getClassLoader().getResource("views/Root.fxml"));
			ResourceBundle bundle = ResourceBundle.getBundle("bundles.mcs");
			loader.setResources(bundle);
			rootLayout = (BorderPane) loader.load();
			Scene scene = new Scene(rootLayout);
			primaryStage.setScene(scene);
			primaryStage.setMinWidth(800);
			primaryStage.setMinHeight(550);
			primaryStage.show();

			// Files named on the commandline are added immediately
			getParameters().getUnnamed().stream().map(File::new).forEach(rootController::addFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}

package at.fhtw.mcs;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import at.fhtw.mcs.controller.RootController;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

	private Stage primaryStage;
	private BorderPane rootLayout;
	private HostServices hostServices;

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		this.hostServices = getHostServices();
		initRootLayout();

	}

	/**
	 * Initialize Root Layout
	 */
	public void initRootLayout() {
		try {
			FXMLLoader loader = new FXMLLoader();
			RootController rootController = new RootController(primaryStage, hostServices);
			loader.setController(rootController);
			loader.setLocation(getClass().getClassLoader().getResource("views/Root.fxml"));
			ResourceBundle bundle = ResourceBundle.getBundle("bundles.mcs");
			loader.setResources(bundle);
			rootLayout = (BorderPane) loader.load();
			Scene scene = new Scene(rootLayout);
			primaryStage.setScene(scene);
			primaryStage.setMinWidth(800);
			primaryStage.setMinHeight(550);
			rootController.sceneInitialization(scene);
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

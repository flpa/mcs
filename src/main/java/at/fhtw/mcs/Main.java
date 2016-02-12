package at.fhtw.mcs;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import at.fhtw.mcs.controller.RootController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

	private Stage primaryStage;
	private StackPane rootLayout;

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
			rootLayout = (StackPane) loader.load();
			Scene scene = new Scene(rootLayout);
			primaryStage.setScene(scene);
			primaryStage.setMinWidth(800);
			primaryStage.setMinHeight(550);
			rootController.sceneInitialization(scene);
			primaryStage.show();

			// Files named on the commandline are added immediately
			List<File> files = getParameters().getUnnamed().stream().map(File::new).collect(Collectors.toList());
			rootController.addFiles(files);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}

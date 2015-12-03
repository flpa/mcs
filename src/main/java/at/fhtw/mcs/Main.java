package at.fhtw.mcs;

import java.io.IOException;
import java.util.Locale;
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
	private RootController rootController;

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
			loader.setController(new RootController(primaryStage));
			loader.setLocation(getClass().getResource("../../../views/Root.fxml"));
			ResourceBundle bundle = ResourceBundle.getBundle("bundles.mcs");
			loader.setResources(bundle);
			rootLayout = (BorderPane) loader.load();
			rootController = (RootController) loader.getController();

			Scene scene = new Scene(rootLayout);
			primaryStage.setTitle(bundle.getString("app.title"));
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
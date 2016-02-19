package at.fhtw.mcs.ui;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

/**
 * {@link ProgressOverlay} is a helper class to create overlays consisting of a
 * progress indicator around a label with a given message. It requires the
 * application to use a {@link StackPane} as root container, so that the overlay
 * items can be added in front of the regular UI. The regular UI is disabled
 * during the overlay. <br/>
 * <br/>
 * <img src=
 * "https://raw.githubusercontent.com/flpa/mcs/screenshots/docs/screenshots/progress-overlay.png"
 * />
 *
 */
public class ProgressOverlay {
	private StackPane rootPane;
	private ProgressIndicator indicator;
	private Label label;

	public ProgressOverlay(StackPane rootPane, String message) {
		this.rootPane = rootPane;
		this.label = new Label(message);
		this.indicator = new ProgressIndicator();
		this.indicator.setPrefSize(100, 100);
		this.indicator.setMaxSize(100, 100);
	}

	public void show() {
		ObservableList<Node> children = rootPane.getChildren();
		children.get(0).setDisable(true);
		children.add(0, indicator);
		children.add(0, label);
	}

	public void hide() {
		ObservableList<Node> children = rootPane.getChildren();
		children.remove(0, 2);
		children.get(0).setDisable(false);
	}
}

package at.fhtw.mcs.util;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

/**
 * Largely copied from
 * https://stackoverflow.com/questions/22424082/drag-and-drop-vbox-element-with-
 * show-snapshot-in-javafx
 *
 */
public class DragDropSupport {
	public static void register(final VBox root) {
		// in case user drops node in blank space in root:
		root.setOnMouseDragReleased(event -> {
			removePreview(root);
			int indexOfDraggingNode = root.getChildren().indexOf(event.getGestureSource());
			rotateNodes(root, indexOfDraggingNode, root.getChildren().size() - 1);
		});

		root.getChildren().addListener((ListChangeListener<Node>) change -> {
			while (change.next()) {
				if (change.wasAdded()) {
					change.getAddedSubList().forEach(node -> DragDropSupport.addWithDragging(root, node));
				}
			}
		});
	}

	private static void addWithDragging(final VBox root, final Node child) {
		child.setOnDragDetected(event -> {
			addPreview(root, child);
			child.startFullDrag();
		});

		// next two handlers just an idea how to show the drop target visually:
		child.setOnMouseDragEntered(event -> {
			child.setStyle("-fx-background-color: #ffffa0;");
		});
		child.setOnMouseDragExited(event -> {
			child.setStyle("");
		});

		child.setOnMouseDragReleased(event -> {
			removePreview(root);
			child.setStyle("");
			int indexOfDraggingNode = root.getChildren().indexOf(event.getGestureSource());
			int indexOfDropTarget = root.getChildren().indexOf(child);
			rotateNodes(root, indexOfDraggingNode, indexOfDropTarget);
			event.consume();
		});
	}

	private static void rotateNodes(final VBox root, final int indexOfDraggingNode, final int indexOfDropTarget) {
		if (indexOfDraggingNode >= 0 && indexOfDropTarget >= 0) {
			final Node node = root.getChildren().remove(indexOfDraggingNode);
			root.getChildren().add(indexOfDropTarget, node);
		}
	}

	private static void addPreview(final VBox root, final Node label) {
		ImageView imageView = new ImageView(label.snapshot(null, null));
		imageView.setManaged(false);
		imageView.setMouseTransparent(true);
		root.getChildren().add(imageView);
		root.setUserData(imageView);
		root.setOnMouseDragged(event -> {
			imageView.relocate(event.getX(), event.getY());
		});
	}

	private static void removePreview(final VBox root) {
		root.setOnMouseDragged(null);
		root.getChildren().remove(root.getUserData());
		root.setUserData(null);
	}
}

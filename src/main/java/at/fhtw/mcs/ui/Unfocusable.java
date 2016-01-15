package at.fhtw.mcs.ui;

import javafx.scene.Node;

/**
 * Interface for {@link Node}s that are not focusable. This means that instead
 * of {@link Node#requestFocus()}, they pass the focus on to the root element of
 * the scene.
 */
interface Unfocusable {
	default void passOnFocus(Node n) {
		n.getScene().getRoot().requestFocus();
	}
}

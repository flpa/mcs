package at.fhtw.mcs.ui;

import javafx.scene.control.ScrollPane;

/**
 * @see Unfocusable
 */
public class UnfocusableScrollPane extends ScrollPane implements Unfocusable {
	@Override
	public void requestFocus() {
		passOnFocus(this);
	}
}

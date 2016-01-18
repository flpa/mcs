package at.fhtw.mcs.ui;

import javafx.scene.control.Button;

/**
 * @see Unfocusable
 */
public class UnfocusableButton extends Button implements Unfocusable {
	@Override
	public void requestFocus() {
		passOnFocus(this);
	}
}

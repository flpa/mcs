package at.fhtw.mcs.ui;

import javafx.scene.control.RadioButton;

/**
 * @see Unfocusable
 */
public class UnfocusableRadioButton extends RadioButton implements Unfocusable {
	@Override
	public void requestFocus() {
		passOnFocus(this);
	}
}

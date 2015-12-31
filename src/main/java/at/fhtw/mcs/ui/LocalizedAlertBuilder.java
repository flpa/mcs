package at.fhtw.mcs.ui;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

/**
 * The idea of {@link LocalizedAlertBuilder} is to simplify the creation of
 * localized (translated) Java FX {@link Alert}s by relying on
 * "convention over configuration". The simplest usage only needs a
 * {@link ResourceBundle} (for looking up translations), a prefix for the bundle
 * messages (for guessing the messages for title, headerText and contentText)
 * and the {@link AlertType} (for creating the resulting {@link Alert}).
 * <p/>
 * <h5>Example</h5>
 * 
 * <pre>
 * new LocalizedAlertBuilder(bundle, "alert.myWarning.", AlertType.WARNING).build().showAndWait();
 * </pre>
 * 
 * This line show a modal Alert of Type WARNING. Title, headerText and
 * contentText of the dialog will be fetched from the <code>bundle</code> using
 * the keys
 * 
 * <pre>
 * alert.myWarning.title
 * alert.myWarning.header
 * alert.myWarning.content
 * </pre>
 * 
 * <h5>Messages with format parameters</h5> It can be necessary to insert
 * parameters into messages, for example the name of the file that caused an
 * error. For this purpose, {@link LocalizedAlertBuilder} offers methods to set
 * format parameters. These parameters will be inserted into the messages using
 * the {@link MessageFormat} class. See for example
 * {@link #setContentFormatParameters(Object...)}
 * 
 * <h5>Overriding the defaults</h5> It is possible to override the message keys
 * as well as the final text of a property. See {@link #setContentKey(String)}
 * and {@link #setContentText(String)}. <b>Note: The message keys will still be
 * concatenated to the message prefix!</b>
 * 
 * @author florian.patzl@technikum-wien.at
 *
 */
public class LocalizedAlertBuilder {
	private static final String NOT_SET = new String("NOT SET");

	private final ResourceBundle translations;
	private final String messagePrefix;
	private final AlertType alertType;

	private String titleKey = "title";
	private String headerKey = "header";
	private String contentKey = "content";

	private String title = NOT_SET;
	private String headerText = NOT_SET;
	private String contentText = NOT_SET;

	private Object[] titleFormatParameters = new Object[0];
	private Object[] headerFormatParameters = new Object[0];
	private Object[] contentFormatParameters = new Object[0];

	private ButtonType[] buttons = new ButtonType[0];

	public LocalizedAlertBuilder(ResourceBundle translations, String messagePrefix, AlertType alertType) {
		this.translations = translations;
		this.messagePrefix = messagePrefix;
		this.alertType = alertType;
	}

	public Alert build() {
		String finalContentText = givenTextOrMessage(contentText, contentKey, contentFormatParameters);
		Alert alert = new Alert(alertType, finalContentText, buttons);

		alert.setTitle(givenTextOrMessage(title, titleKey, titleFormatParameters));
		alert.setHeaderText(givenTextOrMessage(headerText, headerKey, headerFormatParameters));

		return alert;
	}

	private String givenTextOrMessage(String text, String messageKey, Object[] formatParameters) {
		return text != NOT_SET ? text
				: MessageFormat.format(translations.getString(messagePrefix + messageKey), formatParameters);
	}

	public void setButtons(ButtonType... buttons) {
		this.buttons = buttons;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setHeaderText(String headerText) {
		this.headerText = headerText;
	}

	public void setContentText(String contentText) {
		this.contentText = contentText;
	}

	public void setTitleKey(String titleKey) {
		this.titleKey = titleKey;
	}

	public void setHeaderKey(String headerKey) {
		this.headerKey = headerKey;
	}

	public void setContentKey(String contentKey) {
		this.contentKey = contentKey;
	}

	public void setTitleFormatParameters(Object... titleFormatParameters) {
		this.titleFormatParameters = titleFormatParameters;
	}

	public void setHeaderFormatParameters(Object... headerFormatParameters) {
		this.headerFormatParameters = headerFormatParameters;
	}

	public void setContentFormatParameters(Object... contentFormatParameters) {
		this.contentFormatParameters = contentFormatParameters;
	}
}

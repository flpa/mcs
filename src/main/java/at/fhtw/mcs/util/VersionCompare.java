package at.fhtw.mcs.util;

import java.io.IOException;
import java.util.ResourceBundle;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class VersionCompare implements Runnable {
	private String newestVersion;
	private ResourceBundle bundle;

	public VersionCompare(ResourceBundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public void run() {
		try {
			compareVersions();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void compareVersions() throws IOException {
		Document doc = Jsoup.connect("https://github.com/flpa/mcs/releases").get();
		Elements versions = doc.getElementsByClass("css-truncate-target");

		newestVersion = versions.get(0).text();
		String version = bundle.getString("project.version");
		// TODO: delete the following Variable, it's just for testing
		// version = "bla";

		if (!version.equals(newestVersion)) {
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle(bundle.getString("alert.newVersionAvailable.title"));
			alert.setHeaderText(bundle.getString("alert.newVersionAvailable.header"));
			alert.setContentText(bundle.getString("alert.newVersionAvailable.content"));

			Elements infos = doc.getElementsByClass("markdown-body");

			String info = infos.get(0).text();

			TextArea textArea = new TextArea(info);
			textArea.setEditable(false);
			textArea.setWrapText(true);

			textArea.setMaxWidth(Double.MAX_VALUE);
			textArea.setMaxHeight(Double.MAX_VALUE);
			GridPane.setVgrow(textArea, Priority.ALWAYS);
			GridPane.setHgrow(textArea, Priority.ALWAYS);

			GridPane expContent = new GridPane();
			expContent.setMaxWidth(Double.MAX_VALUE);
			expContent.add(textArea, 0, 1);

			// Set expandable Exception into the dialog pane.
			alert.getDialogPane().setExpandableContent(expContent);
			alert.getDialogPane().setExpanded(true);

			alert.showAndWait();
		}
	}

}

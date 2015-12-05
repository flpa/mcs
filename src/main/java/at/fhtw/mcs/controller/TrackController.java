package at.fhtw.mcs.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Vector;

import at.fhtw.mcs.model.Track;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.text.Text;

/**
 * Controller class for Track.fxml
 */
public class TrackController implements Initializable {
	@FXML
	private Text textTrackName;

	@FXML
	private LineChart<Number, Number> lineChartWaveform;

	private Track track;

	public TrackController(Track track) {
		this.track = track;
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		textTrackName.setText(track.getFilename());

		XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();

		Vector<float[]> tempData = track.getAudioData();

		int x = 0;

		for (int i = 1; i <= tempData.size(); i++) {

			if (tempData.elementAt(i - 1) == null) {
				break;
			}

			for (int j = 0; j < tempData.elementAt(i - 1).length; j++) {

				if (j % 2000 == 0) {
					float leftChannel = tempData.elementAt(i - 1)[j];
					float rightChannel = tempData.elementAt(i - 1)[j + 1];
					float mean = (leftChannel + rightChannel) / 2;

					series.getData().add(new XYChart.Data<Number, Number>(x, mean));
					x++;
				}
			}
		}

		lineChartWaveform.getData().add(series);

	}
}

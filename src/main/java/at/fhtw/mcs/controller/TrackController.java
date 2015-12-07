package at.fhtw.mcs.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Vector;

import at.fhtw.mcs.model.Track;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
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
		int OFFSET;
		int shortTrackBoundary = 30000000;
		int mediumTrackBoundary = 180000000;
		if (track.getTotalMicroseconds() < shortTrackBoundary) {
			OFFSET = 64;
		} else if (track.getTotalMicroseconds() < mediumTrackBoundary) {
			OFFSET = 512;
		} else {
			OFFSET = 1024;
		}

		textTrackName.setText(track.getFilename());

		XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();

		Vector<float[]> tempData = track.getAudioData();

		int audioFileLength = track.getLength();
		int x = 0;

		for (int i = 0; i < tempData.size(); i++) {

			if (tempData.elementAt(i) == null) {
				break;
			}

			/**
			 * TODO Ich geh hier momentan hardcoded davon aus, dass wir immer
			 * Stereofiles haben. Das sollt aber eigentlich ned so sein. Schöner
			 * wärs wenn wir die Channel Anzahl auslesen und dann die Funktion
			 * anpassen. Es müsste bei mono auch kein "mean" gebildet werden.
			 */

			for (int j = 0; j < audioFileLength * 2; j++) {

				if (j % OFFSET == 0) {
					float leftChannel = tempData.elementAt(i)[j];
					float rightChannel = tempData.elementAt(i)[j + 1];
					float mean = (leftChannel + rightChannel) / 2;

					series.getData().add(new XYChart.Data<Number, Number>(x, mean));
					x++;
					// System.out.println(x * OFFSET + " : " + audioFileLength *
					// 2);
				}
			}
		}

		lineChartWaveform.getData().add(series);

		lineChartWaveform.getXAxis().setAutoRanging(false);
		((NumberAxis) lineChartWaveform.getXAxis()).setUpperBound(x - 1);
		lineChartWaveform.getYAxis().setAutoRanging(false);
		((NumberAxis) lineChartWaveform.getYAxis()).setLowerBound(-1.0);
		((NumberAxis) lineChartWaveform.getYAxis()).setUpperBound(1.0);

	}
}

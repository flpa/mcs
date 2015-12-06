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

		int audioFileLength = track.getLength();
		int x = 0;

		for (int i = 0; i < tempData.size(); i++) {

			if (tempData.elementAt(i) == null) {
				break;
			}

			for (int j = 0; j < audioFileLength * 2; j++) {

				/**
				 * TODO if the number after % is bigger, the performance will be
				 * better but the waveform will be not as detailed, it also
				 * shouldn't be bigger than the Buffer_size used in
				 * JavaxJavazoomTrack.java
				 */

				if (j % 1024 == 0) {
					float leftChannel = tempData.elementAt(i)[j];
					float rightChannel = tempData.elementAt(i)[j + 1];
					float mean = (leftChannel + rightChannel) / 2;

					series.getData().add(new XYChart.Data<Number, Number>(x, mean));
					x++;
					System.out.println(x * 1024 + " : " + audioFileLength * 2);
				}
			}
		}

		lineChartWaveform.getData().add(series);

	}
}

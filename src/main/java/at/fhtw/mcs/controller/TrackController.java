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
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.text.Text;

/**
 * Controller class for Track.fxml
 */
public class TrackController implements Initializable {
	@FXML
	private Text textTrackName;
	@FXML
	private LineChart<Number, Number> lineChartWaveform;
	@FXML
	private RadioButton radioButtonActiveTrack;
	@FXML
	private Button buttonMoveUp;
	@FXML
	private Button buttonMoveDown;
	@FXML
	private Button buttonDelete;

	private ToggleGroup toggleGroup;
	private Track track;

	public TrackController(Track track, ToggleGroup toggleGroup) {
		this.track = track;
		this.toggleGroup = toggleGroup;
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		int offset = Math.round(track.getLength() / 2000);
		System.out.println("Offset: " + offset);

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
			 * TODO momentan ist die Zeichnung auf 2000 Werte begrenzt, bei
			 * längeren Files kommt dadurch kein schönes Bild zustande, müssen
			 * wir uns noch anschauen
			 */

			for (int j = 0; j < audioFileLength * track.getNumberOfChannels(); j++) {

				if (j % offset == 0) {
					float mean = 0;
					float leftChannel = tempData.elementAt(i)[j];
					if (track.getNumberOfChannels() == 2) {
						float rightChannel = tempData.elementAt(i)[j + 1];
						mean = (leftChannel + rightChannel) / 2;
					} else {
						mean = leftChannel;
					}
					series.getData().add(new XYChart.Data<Number, Number>(x, mean));
					x++;
				}
			}
		}

		lineChartWaveform.getData().add(series);

		lineChartWaveform.getXAxis().setAutoRanging(false);
		((NumberAxis) lineChartWaveform.getXAxis()).setUpperBound(x - 1);
		lineChartWaveform.getYAxis().setAutoRanging(false);
		((NumberAxis) lineChartWaveform.getYAxis()).setLowerBound(-1.0);
		((NumberAxis) lineChartWaveform.getYAxis()).setUpperBound(1.0);

		radioButtonActiveTrack.setToggleGroup(toggleGroup);
		radioButtonActiveTrack.setUserData(track);
		radioButtonActiveTrack.setSelected(track.isMuted() == false);
	}

	public Button getButtonMoveUp() {
		return this.buttonMoveUp;
	}

	public Button getButtonMoveDown() {
		return this.buttonMoveDown;
	}

	public Button getButtonDelete() {
		return this.buttonDelete;
	}

	public RadioButton getRadioButtonActiveTrack() {
		return this.radioButtonActiveTrack;
	}
}

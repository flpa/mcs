package at.fhtw.mcs.controller;

import java.net.URL;
import java.util.ResourceBundle;

import at.fhtw.mcs.model.Track;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.text.Text;

/**
 * Controller class for Track.fxml
 */
public class TrackController implements Initializable {
	@FXML
	private Text textTrackName;
	@FXML
	private Text textDynamicRange;
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
	@FXML
	private TextArea textAreaComment;

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
		textDynamicRange.setText(String.format("%.2f dB", track.getDynamicRange()));

		XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();

		float[] tempData = track.getAudioData();

		int audioFileLength = track.getLength();
		int x = 0;

		/**
		 * TODO momentan ist die Zeichnung auf 2000 Werte begrenzt, bei längeren
		 * Files kommt dadurch kein schönes Bild zustande, müssen wir uns noch
		 * anschauen
		 */

		for (int i = 0; i < audioFileLength * track.getNumberOfChannels(); i += offset) {
			float mean = 0;
			float leftChannel = tempData[i];

			if (track.getNumberOfChannels() == 2) {
				float rightChannel = tempData[i + 1];
				mean = (leftChannel + rightChannel) / 2;
			} else {
				mean = leftChannel;
			}
			series.getData().add(new XYChart.Data<Number, Number>(x, mean));
			x++;
		}

		lineChartWaveform.getData().add(series);

		NumberAxis xAxis = (NumberAxis) lineChartWaveform.getXAxis();
		xAxis.setAutoRanging(false);
		xAxis.setUpperBound(x - 1);
		NumberAxis yAxis = (NumberAxis) lineChartWaveform.getYAxis();
		yAxis.setAutoRanging(false);
		yAxis.setLowerBound(-1.0);
		yAxis.setUpperBound(1.0);

		radioButtonActiveTrack.setToggleGroup(toggleGroup);
		radioButtonActiveTrack.setUserData(track);
		if (toggleGroup.getSelectedToggle() == null) {
			radioButtonActiveTrack.setSelected(true);
		}

		textAreaComment.textProperty().addListener(new ChangeListener<String>() {
			public void changed(ObservableValue<? extends String> value, String previousComment, String newComment) {
				track.setComment(newComment);
			}
		});
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

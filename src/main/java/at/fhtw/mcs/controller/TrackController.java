package at.fhtw.mcs.controller;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ResourceBundle;

import at.fhtw.mcs.model.Track;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

/**
 * Controller class for Track.fxml
 */
public class TrackController implements Initializable {
	private static final int GRAPH_POINT_COUNT = 1750;
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
	@FXML
	private Canvas canvasTrack;
	@FXML
	private AnchorPane anchorPaneTrackPane;

	private ToggleGroup toggleGroup;
	private Track track;
	private long longestTrackFrameLength;

	public TrackController(Track track, ToggleGroup toggleGroup, long longestTrackFrameLength) {
		this.track = track;
		this.toggleGroup = toggleGroup;
		this.longestTrackFrameLength = longestTrackFrameLength;
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		textTrackName.setText(track.getName());
		textDynamicRange.setText(String.format("%.2f dB", track.getDynamicRange()));

		drawTrack();

		// TODO Canvas isn't getting smaller

		anchorPaneTrackPane.widthProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				// System.out.println(oldValue + "->" + newValue);
				canvasTrack.setWidth(textAreaComment.getWidth());
				if ((double) oldValue > (double) newValue) {
					// System.out.println(canvasTrack.getWidth());
					canvasTrack.setWidth(canvasTrack.getWidth() - ((double) oldValue - (double) newValue));
					// System.out.println(canvasTrack.getWidth());
				}
			}
		});

		radioButtonActiveTrack.setToggleGroup(toggleGroup);
		radioButtonActiveTrack.setUserData(new WeakReference<>(track));
		if (toggleGroup.getSelectedToggle() == null) {
			radioButtonActiveTrack.setSelected(true);
		}

		textAreaComment.textProperty().bindBidirectional(track.commentProperty());
	}

	public void drawTrack() {
		int offset = Math.round(longestTrackFrameLength / GRAPH_POINT_COUNT);
		offset = (int) (offset * (track.getSampleRate() / 44100));

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

		// Fill rest with zeroes
		// TODO: it seems like we're actually drawing 2 x 2000 points??
		for (; x < GRAPH_POINT_COUNT * track.getNumberOfChannels(); x++) {
			series.getData().add(new XYChart.Data<Number, Number>(x, 0));
		}

		lineChartWaveform.getData().clear();
		lineChartWaveform.getData().add(series);

		NumberAxis xAxis = (NumberAxis) lineChartWaveform.getXAxis();
		xAxis.setAutoRanging(false);
		xAxis.setUpperBound(x - 1);
		NumberAxis yAxis = (NumberAxis) lineChartWaveform.getYAxis();
		yAxis.setAutoRanging(false);
		yAxis.setLowerBound(-1.0);
		yAxis.setUpperBound(1.0);
	}

	public void setLongestTrackFrameLength(long longestTrackFrameLength) {
		if (longestTrackFrameLength != this.longestTrackFrameLength) {
			this.longestTrackFrameLength = longestTrackFrameLength;
			drawTrack();
		}
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

	public Track getTrack() {
		return this.track;
	}

	public LineChart<Number, Number> getChart() {
		return this.lineChartWaveform;
	}

	public void setRadioButtonActive() {
		this.radioButtonActiveTrack.fire();
	}

	public AnchorPane getAnchorPane() {
		return this.anchorPaneTrackPane;
	}

	public Canvas getCanvas() {
		return this.canvasTrack;
	}
}

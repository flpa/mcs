package at.fhtw.mcs.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

import at.fhtw.mcs.model.Format;
import at.fhtw.mcs.model.Track;
import at.fhtw.mcs.util.AudioOuput;
import at.fhtw.mcs.util.TrackFactory;
import at.fhtw.mcs.util.TrackFactory.UnsupportedFormatException;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Controller class for Root.fxml
 */
public class RootController implements Initializable {

	/*
	 * Where to move those? ResourceBundles?
	 */
	private static final String ICON_PAUSE = "||";
	private static final String ICON_PLAY = "▶";

	@FXML
	private VBox vboxTracks;
	@FXML
	private Menu menuOutputDevices;
	@FXML
	private MenuItem menuItemQuit;
	@FXML
	private MenuItem menuItemAddTrack;
	@FXML
	private MenuItem menuItemAbout;
	@FXML
	private Button buttonPlayPause;
	@FXML
	private Button buttonStop;
	@FXML
	private Button buttonAddTrack;
	@FXML
	private Text textCurrentTime;
	@FXML
	private Text textTotalTime;
	@FXML
	private ProgressBar progressBarTime;
	@FXML
	private ScrollPane scrollPaneTracks;
	@FXML
	private Rectangle rectangleSpacer;

	private ToggleGroup toggleGroupActiveTrack = new ToggleGroup();
	private ResourceBundle bundle;
	private Stage stage;

	private List<Track> tracks = new ArrayList<>();
	private List<TrackController> trackControllers = new ArrayList<>();
	private List<List<Button>> moveButtonList = new ArrayList<>();
	private List<Button> deleteButtonList = new ArrayList<>();

	// TODO: config parameter
	private long updateFrequencyMs = 100;

	public RootController(Stage stage) {
		this.stage = stage;
	}

	@Override
	public void initialize(URL viewSource, ResourceBundle translations) {
		this.bundle = translations;

		// 'x -> functionCall' is a minimalistic Java8 lambda
		menuItemQuit.setOnAction(e -> Platform.exit());
		menuItemAddTrack.setOnAction(this::handleAddTrack);
		menuItemAbout.setOnAction(this::handleAbout);

		// TODO: inline lambdas vs methods?
		buttonPlayPause.setOnAction(e -> {
			tracks.forEach(Track::togglePlayPause);
			buttonPlayPause.setText(ICON_PLAY.equals(buttonPlayPause.getText()) ? ICON_PAUSE : ICON_PLAY);
		});
		buttonStop.setOnAction(e -> {
			tracks.forEach(Track::stop);
			buttonPlayPause.setText(ICON_PLAY);
		});
		buttonAddTrack.setOnAction(this::handleAddTrack);

		ToggleGroup toggleGroupOutputDevice = new ToggleGroup();

		// @formatter:off
		Arrays.stream(AudioSystem.getMixerInfo()).filter(RootController::isOutputMixerInfo).forEach(info -> {
			RadioMenuItem radio = new RadioMenuItem();
			radio.setText(String.format("%s (%s)", info.getName(), info.getDescription()));
			radio.setUserData(info);
			radio.setToggleGroup(toggleGroupOutputDevice);
			radio.setSelected(info.equals(AudioOuput.getSelectedMixerInfo()));
			menuOutputDevices.getItems().add(radio);
		});
		// @formatter:on

		toggleGroupOutputDevice.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			public void changed(ObservableValue<? extends Toggle> value, Toggle previousSelection,
					Toggle newSelection) {
				/*
				 * When modifying grouped RadioMenuItems, this is invoked twice:
				 * 1) oldValue and null 2) null and newValue
				 */
				if (newSelection != null) {
					AudioOuput.setSelectedMixerInfo((Mixer.Info) newSelection.getUserData());
					tracks.forEach(Track::reload);
				}
			}
		});

		toggleGroupActiveTrack.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			public void changed(ObservableValue<? extends Toggle> value, Toggle previousSelection,
					Toggle newSelection) {
				if (previousSelection != null) {
					((Track) previousSelection.getUserData()).mute();
				}
				((Track) newSelection.getUserData()).unmute();
			}
		});

		scrollPaneTracks.viewportBoundsProperty().addListener(new ChangeListener<Bounds>() {
			public void changed(ObservableValue<? extends Bounds> value, Bounds previousBounds, Bounds newBounds) {
				double scrollBarWidth = scrollPaneTracks.getWidth() - scrollPaneTracks.getViewportBounds().getWidth();
				rectangleSpacer.setWidth(scrollBarWidth);
			}
		});

	}

	private static boolean isOutputMixerInfo(Mixer.Info info) {
		return AudioSystem.getMixer(info).isLineSupported(new Line.Info(Clip.class));
	}

	private void updateTime() {
		if (tracks.isEmpty()) {
			return;
		}
		// TODO: for now, we'll assume only checking the first track is ok.
		Track track = tracks.get(0);
		long currentMicroseconds = track.getCurrentMicroseconds();
		long totalMicroseconds = track.getTotalMicroseconds();
		progressBarTime.setProgress((double) currentMicroseconds / totalMicroseconds);
		textCurrentTime.setText(formatTimeString(currentMicroseconds));

		// TODO: reset playPause button; doesn't work
		// if (currentMicroseconds == totalMicroseconds) {
		// buttonPlayPause.setText(ICON_PLAY);
		// }
	}

	private void handleAddTrack(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		/*
		 * TODO: should restrict file types! but maybe don't hardcode, rather
		 * 'ask' a responsible class what file types are allowed?
		 */

		chooser.setTitle("TRANSLATE ME");
		File file = chooser.showOpenDialog(stage);
		if (file == null) {
			return;
		}

		Track track;
		try {
			track = TrackFactory.loadTrack(file.getAbsolutePath());
		} catch (UnsupportedFormatException e) {
			this.showErrorUnsupportedFormat(e.getFormat(), e.getAudioFormat());
			return;
		}

		// Things to be done for first track
		if (tracks.isEmpty()) {
			long totalMicroseconds = track.getTotalMicroseconds();
			String timeString = formatTimeString(totalMicroseconds);
			textTotalTime.setText(timeString);

			startTimeUpdateThread();
		} else {
			track.mute();
		}

		loadTrackUi(track);
		buttonPlayPause.setDisable(false);
		buttonStop.setDisable(false);

		tracks.add(track);

		addButtons();
		setLoudnessLevel();
		// setMoveButtons();
		setButtonsEventHandler();

		// handles the case if a longer track is loaded after a shorter one
		long longest = 0;
		for (Track t : tracks) {
			if (t.getTotalMicroseconds() > longest) {
				longest = t.getTotalMicroseconds();
			}

		}
		String timeString = formatTimeString(longest);
		textTotalTime.setText(timeString);
	}

	private void loadTrackUi(Track track) {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setController(new TrackController(track, toggleGroupActiveTrack));
			loader.setLocation(getClass().getClassLoader().getResource("views/Track.fxml"));
			loader.setResources(bundle);
			trackControllers.add(loader.getController());

			vboxTracks.getChildren().add(loader.load());
		} catch (IOException e) {
			// TODO: better exception handling
			e.printStackTrace();
		}
	}

	private void startTimeUpdateThread() {
		Timer timer = new Timer(true);
		/*
		 * Reading the documentation of timer.schedule(...), it seems like
		 * there's no danger of timer-execution-congestion when a time
		 * invocation blocks: "[...]each execution is scheduled relative to the
		 * actual execution time of the previous execution."
		 */
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				long prevMillis = System.currentTimeMillis();
				updateTime();
				long elapsedMs = System.currentTimeMillis() - prevMillis;

				if (elapsedMs >= updateFrequencyMs) {
					System.err.println(
							String.format("Warning: Time update (%dms) took longer than the update frequency (%dms).",
									elapsedMs, updateFrequencyMs));
				}
			}
		}, 0, updateFrequencyMs);
	}

	private String formatTimeString(long totalMicroseconds) {
		long minutes = TimeUnit.MICROSECONDS.toMinutes(totalMicroseconds);
		long seconds = TimeUnit.MICROSECONDS.toSeconds(totalMicroseconds) % 60;

		return String.format("%d:%02d", minutes, seconds);
	}

	private void handleAbout(ActionEvent event) {
		Alert alertAbout = new Alert(AlertType.INFORMATION);
		alertAbout.setTitle(bundle.getString("about.title"));
		alertAbout.setHeaderText(null);
		alertAbout.setContentText(bundle.getString("about.contentText"));

		((Label) alertAbout.getDialogPane().getChildren().get(1)).setWrapText(false);
		alertAbout.getDialogPane().setPrefHeight(Region.USE_COMPUTED_SIZE);
		// TODO: auto-resize to content
		alertAbout.getDialogPane().setPrefWidth(700);

		alertAbout.showAndWait();
	}

	private void showErrorUnsupportedFormat(Format format, AudioFormat audioFormat) {
		Alert alertError = new Alert(AlertType.ERROR);
		alertError.setTitle(bundle.getString("errorUnsupportedFormat.title"));
		alertError.setHeaderText(null);

		String errorText = bundle.getString(determineErrorDescriptionForFormat(format, audioFormat));
		errorText += bundle.getString("errorUnsupportedFormat.supportedFormats");
		alertError.setContentText(errorText);

		alertError.getDialogPane().setPrefHeight(Region.USE_COMPUTED_SIZE);
		alertError.getDialogPane().setPrefWidth(700);

		alertError.showAndWait();
	}

	private String determineErrorDescriptionForFormat(Format format, AudioFormat audioFormat) {
		switch (format) {
			case AIFF:
			case WAV:
				if (audioFormat.getSampleSizeInBits() == 24) {
					return "errorUnsupportedFormat.content24bit";
				} else {
					return "errorUnsupportedFormat.contentDefault";
				}
			case MP3:
				return "errorUnsupportedFormat.contentMp3";
			default:
				return "errorUnsupportedFormat.contentDefault";
		}
	}

	private void addButtons() {
		moveButtonList.clear();
		deleteButtonList.clear();
		for (int i = 0; i < trackControllers.size(); i++) {
			// deleteButton
			deleteButtonList.add(trackControllers.get(i).getButtonDelete());

			// moveButtons
			List<Button> tempList = new ArrayList<>();
			tempList.add(trackControllers.get(i).getButtonMoveUp());
			tempList.add(trackControllers.get(i).getButtonMoveDown());
			moveButtonList.add(tempList);
		}
	}

	private void setLoudnessLevel() {
		// anpassen der lautstärke
		float min = 0;

		for (Track trackiterator : tracks) {
			if (min > trackiterator.getLoudness()) {
				min = trackiterator.getLoudness();
			}
		}

		for (Track track2 : tracks) {
			track2.setVolume(min);
		}
	}

	private void setMoveButtons() {
		for (int i = 0; i < tracks.size(); i++) {
			if (i == 0) {
				moveButtonList.get(i).get(0).setDisable(true);
				moveButtonList.get(i).get(1).setDisable(false);
			} else if (i < tracks.size() - 1) {
				moveButtonList.get(i).get(0).setDisable(false);
				moveButtonList.get(i).get(1).setDisable(false);
			}
			if (i == tracks.size() - 1) {
				moveButtonList.get(i).get(1).setDisable(true);
			}
		}
	}

	private void setButtonsEventHandler() {
		for (int i = 0; i < moveButtonList.size(); i++) {
			final int trackNumber = i;
			deleteButtonList.get(i).setOnAction(e -> {
				deleteTrack(trackNumber);
			});
			for (int j = 0; j < moveButtonList.get(i).size(); j++) {
				final int buttonNumber = j;
				moveButtonList.get(i).get(j).setOnAction(e -> {
					if (buttonNumber != 1) {
						moveUp(trackNumber);
					} else {
						moveDown(trackNumber);
					}
				});
			}
		}
	}

	private void deleteTrack(int number) {
		// System.out.println(number);

		String trackName = tracks.get(number).getFilename();
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(bundle.getString("alert.deleteTrackTitle"));
		alert.setHeaderText(trackName + " " + bundle.getString("alert.deleteTrackHeader"));
		alert.setContentText(bundle.getString("alert.deleteTrackContent"));

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			for (Track track : tracks) {
				track.stop();
			}
			vboxTracks.getChildren().remove(number);
			tracks.remove(number);
			trackControllers.remove(number);
			moveButtonList.remove(number);
			deleteButtonList.remove(number);

			addButtons();
			// setMoveButtons();
			setButtonsEventHandler();
			setLoudnessLevel();
			if (tracks.size() > 0) {
				trackControllers.get(0).getRadioButtonActiveTrack().fire();
			}
		}
	}

	private void moveUp(int number) {
		// System.out.println("number: " + number);
		if (number != 0) {
			List<Node> tempVboxTracks = new ArrayList<>();
			List<TrackController> tempTrackController = new ArrayList<>();
			List<Track> tempTracks = new ArrayList<>();

			for (int i = 0; i < vboxTracks.getChildren().size(); i++) {
				if (i == number - 1) {
					tempVboxTracks.add(vboxTracks.getChildren().get(i + 1));
					tempTrackController.add(trackControllers.get(i + 1));
					tempTracks.add(tracks.get(i + 1));
				} else if (i == number) {
					tempVboxTracks.add(vboxTracks.getChildren().get(i - 1));
					tempTrackController.add(trackControllers.get(i - 1));
					tempTracks.add(tracks.get(i - 1));
				} else {
					tempVboxTracks.add(vboxTracks.getChildren().get(i));
					tempTrackController.add(trackControllers.get(i));
					tempTracks.add(tracks.get(i));
				}
			}

			vboxTracks.getChildren().clear();
			trackControllers.clear();
			tracks.clear();

			for (int i = 0; i < tempVboxTracks.size(); i++) {
				vboxTracks.getChildren().add(tempVboxTracks.get(i));
				trackControllers.add(tempTrackController.get(i));
				tracks.add(tempTracks.get(i));
			}

			addButtons();
			// setMoveButtons();
			setButtonsEventHandler();
		}
	}

	private void moveDown(int number) {
		// System.out.println("number: " + number);
		if (number != tracks.size() - 1) {
			List<Node> tempVboxTracks = new ArrayList<>();
			List<TrackController> tempTrackController = new ArrayList<>();
			List<Track> tempTracks = new ArrayList<>();

			for (int i = 0; i < vboxTracks.getChildren().size(); i++) {
				if (i == number + 1) {
					tempVboxTracks.add(vboxTracks.getChildren().get(i - 1));
					tempTrackController.add(trackControllers.get(i - 1));
					tempTracks.add(tracks.get(i - 1));
				} else if (i == number) {
					tempVboxTracks.add(vboxTracks.getChildren().get(i + 1));
					tempTrackController.add(trackControllers.get(i + 1));
					tempTracks.add(tracks.get(i + 1));
				} else {
					tempVboxTracks.add(vboxTracks.getChildren().get(i));
					tempTrackController.add(trackControllers.get(i));
					tempTracks.add(tracks.get(i));
				}
			}

			vboxTracks.getChildren().clear();
			trackControllers.clear();
			tracks.clear();

			for (int i = 0; i < tempVboxTracks.size(); i++) {
				vboxTracks.getChildren().add(tempVboxTracks.get(i));
				trackControllers.add(tempTrackController.get(i));
				tracks.add(tempTracks.get(i));
			}

			addButtons();
			// setMoveButtons();
			setButtonsEventHandler();
		}
	}

}

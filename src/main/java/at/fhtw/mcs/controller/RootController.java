package at.fhtw.mcs.controller;

import static at.fhtw.mcs.util.NullSafety.emptyListIfNull;
import static java.util.Comparator.comparing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
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
import javax.xml.bind.JAXBException;

import at.fhtw.mcs.model.Format;
import at.fhtw.mcs.model.Project;
import at.fhtw.mcs.model.Track;
import at.fhtw.mcs.ui.LocalizedAlertBuilder;
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
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
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
	private CheckMenuItem checkMenuItemSyncronizeStartPoints;
	@FXML
	private Menu menuOutputDevices;
	@FXML
	private MenuItem menuItemQuit;
	@FXML
	private MenuItem menuItemNewProject;
	@FXML
	private MenuItem menuItemOpenProject;
	@FXML
	private MenuItem menuItemSaveProject;
	@FXML
	private MenuItem menuItemSaveProjectAs;
	@FXML
	private MenuItem menuItemCloseProject;
	@FXML
	private MenuItem menuItemAddTracks;
	@FXML
	private MenuItem menuItemAbout;
	@FXML
	private Button buttonPlayPause;
	@FXML
	private Button buttonStop;
	@FXML
	private Button buttonAddTracks;
	@FXML
	private Text textCurrentTime;
	@FXML
	private Text textTotalTime;
	@FXML
	private ProgressBar progressBarTime;
	@FXML
	private Slider sliderProgressBarTime;
	@FXML
	private ScrollPane scrollPaneTracks;
	@FXML
	private Rectangle rectangleSpacer;
	@FXML
	private Slider sliderMasterVolume;

	private ToggleGroup toggleGroupActiveTrack = new ToggleGroup();
	private ResourceBundle bundle;
	private Stage stage;

	private List<TrackController> trackControllers = new ArrayList<>();
	private List<List<Button>> moveButtonList = new ArrayList<>();
	private List<Button> deleteButtonList = new ArrayList<>();

	// TODO: could be a configuration parameter?
	private long updateFrequencyMs = 100;
	private int longestTrackFrameLength;
	private long longestTrackMicrosecondsLength;

	// debug variables
	Boolean trackChanged = false;
	int trackChangedChecker = 0;
	private Project project;

	public RootController(Stage stage) {
		this.stage = stage;
	}

	@Override
	public void initialize(URL viewSource, ResourceBundle translations) {
		this.bundle = translations;
		newProject();

		menuItemQuit.setOnAction(e -> afterUnsavedChangesAreHandledDo(Platform::exit));
		menuItemNewProject.setOnAction(e -> afterUnsavedChangesAreHandledDo(this::newProject));
		menuItemOpenProject.setOnAction(e -> afterUnsavedChangesAreHandledDo(this::openProject));
		menuItemSaveProject.setOnAction(e -> this.save());
		menuItemSaveProjectAs.setOnAction(e -> this.saveAs());
		menuItemCloseProject.setOnAction(e -> afterUnsavedChangesAreHandledDo(this::closeProject));

		menuItemAddTracks.setOnAction(this::handleAddTracks);
		menuItemAbout.setOnAction(this::handleAbout);

		// TODO: inline lambdas vs methods?
		buttonPlayPause.setOnAction(e -> {
			getSelectedTrack().ifPresent(Track::togglePlayPause);

			buttonPlayPause.setText(ICON_PLAY.equals(buttonPlayPause.getText()) ? ICON_PAUSE : ICON_PLAY);
		});
		buttonStop.setOnAction(this::handleStop);
		buttonAddTracks.setOnAction(this::handleAddTracks);

		sliderMasterVolume.setMax(1);
		sliderMasterVolume.setMin(0);
		// TODO: check if Volume changes if you alter the value with clicking
		// instead of dragging

		sliderMasterVolume.valueProperty()
				.addListener((observable, oldValue, newValue) -> project.setMasterLevel((double) newValue));

		checkMenuItemSyncronizeStartPoints.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				project.setSynchronizeStartPoints(newValue);
				for (TrackController trackController : trackControllers) {
					trackController.drawTrack();
				}
			}
		});

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
					project.getTracks().forEach(Track::reload);
				}
			}
		});

		toggleGroupActiveTrack.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			public void changed(ObservableValue<? extends Toggle> value, Toggle previousSelection,
					Toggle newSelection) {

				long currentMs = 0;
				boolean wasPlaying = false;
				trackChanged = true;

				if (previousSelection != null) {
					Track prevTrack = (Track) previousSelection.getUserData();
					wasPlaying = prevTrack.isPlaying();
					currentMs = prevTrack.getCurrentMicroseconds();
					prevTrack.pause();
				}

				if (newSelection != null) {
					Track newTrack = (Track) newSelection.getUserData();
					newTrack.setCurrentMicroseconds(currentMs);
					if (wasPlaying) {
						newTrack.play();
					}
				}
			}
		});

		/*
		 * Start the update thread here to prevent multiple threads when adding
		 * a track, deleting it, adding a track [...]
		 */
		startTimeUpdateThread();

		// Eventlistener to change the Playbackpoint
		// TODO fix bug, where timeline is set back when switching between
		// tracks
		sliderProgressBarTime.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if ((double) newValue - (double) oldValue > 2500000 || (double) newValue - (double) oldValue < 0) {
					if (!trackChanged) {
						for (Track track : project.getTracks()) {
							long temp = Math.round((double) newValue);
							track.setCurrentMicroseconds(temp + 250000);
							// System.out.println("valuechange: " + newValue +
							// ":" + oldValue);
						}
					}
				}
			}
		});
	}

	private void afterUnsavedChangesAreHandledDo(Runnable callback) {
		if (project.hasUnsavedChanges() == false || letUserHandleUnsavedChanges()) {
			callback.run();
		}
	}

	private boolean letUserHandleUnsavedChanges() {
		// ButtonData.YES means this is our default button
		ButtonType save = new ButtonType(bundle.getString("alert.unsavedChanges.button.save"), ButtonData.YES);
		ButtonType proceedWithoutSaving = new ButtonType(
				bundle.getString("alert.unsavedChanges.button.proceedWithoutSaving"));

		LocalizedAlertBuilder builder = new LocalizedAlertBuilder(bundle, "alert.unsavedChanges.",
				AlertType.CONFIRMATION);
		builder.setButtons(proceedWithoutSaving, ButtonType.CANCEL, save);
		builder.setHeaderFormatParameters(getProjectName());

		Optional<ButtonType> result = builder.build().showAndWait();
		return result.isPresent() && (result.get() == proceedWithoutSaving || (result.get() == save && save()));
	}

	private String getProjectName() {
		String name = project.getName();
		return name == null ? bundle.getString("project.unnamed") : name;
	}

	private boolean save() {
		if (project.getDirectory() == null) {
			return saveAs();
		}
		return saveAndCatchErrors();
	}

	private boolean saveAs() {
		Optional<File> directory = letUserChooseProjectDirectory();

		if (directory.isPresent()) {
			project.setDirectory(directory.get());
			return saveAndCatchErrors();
		}
		return false;
	}

	private Optional<File> letUserChooseProjectDirectory() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		return Optional.ofNullable(directoryChooser.showDialog(stage));
	}

	private boolean saveAndCatchErrors() {
		boolean success = false;
		try {
			project.save();
			updateApplicationTitle();
			success = true;
		} catch (IOException e) {
			handleSaveError(e);
		}
		return success;
	}

	private void handleSaveError(IOException e) {
		e.printStackTrace();
		new LocalizedAlertBuilder(bundle, "alert.saveFailed.", AlertType.ERROR).build().showAndWait();
	}

	private void newProject() {
		setProject(new Project());
	}

	private void setProject(Project project) {
		removeAllTracks();
		this.project = project;
		updateApplicationTitle();
		loadTrackUis(project.getTracks());
		sliderMasterVolume.setValue(project.getMasterLevel());
		checkMenuItemSyncronizeStartPoints.setSelected(project.isSynchronizeStartPoints());
		project.unsavedChangesProperty().addListener((observable, oldValue, newValue) -> this.updateApplicationTitle());
	}

	private void removeAllTracks() {
		if (this.project != null) {
			int trackCount = this.project.getTracks().size();
			for (int i = 0; i < trackCount; i++) {
				// removing first track until all have been removed
				removeTrack(0);
			}
		}
	}

	private void openProject() {
		Optional<File> chosenDirectory = letUserChooseProjectDirectory();
		if (chosenDirectory.isPresent()) {
			try {
				setProject(Project.load(chosenDirectory.get()));
			} catch (FileNotFoundException | JAXBException e) {
				handleOpenError(e);
			}
		}
	}

	private void handleOpenError(Exception e) {
		e.printStackTrace();

		LocalizedAlertBuilder builder = new LocalizedAlertBuilder(bundle, "alert.loadFailed.", AlertType.ERROR);
		builder.setContentKey(e instanceof FileNotFoundException ? "content.loadError" : "content.parseError");
		builder.build().showAndWait();
	}

	private void closeProject() {
		newProject();
	}

	private void updateApplicationTitle() {
		String format = MessageFormat.format(bundle.getString("app.title"), getProjectName());
		if (project.hasUnsavedChanges()) {
			format += "*";
		}
		stage.setTitle(format);
	}

	private static boolean isOutputMixerInfo(Mixer.Info info) {
		return AudioSystem.getMixer(info).isLineSupported(new Line.Info(Clip.class));
	}

	private Optional<Track> getSelectedTrack() {
		Toggle selectedToggle = toggleGroupActiveTrack.getSelectedToggle();
		if (selectedToggle == null) {
			return Optional.empty();
		}
		return Optional.of((Track) selectedToggle.getUserData());
	}

	private void updateTime() {
		Optional<Track> selectedTrack = getSelectedTrack();
		if (selectedTrack.isPresent() == false) {
			return;
		}

		if (trackChanged && trackChangedChecker > 10) {
			trackChanged = false;
			trackChangedChecker = 0;
		} else if (trackChanged) {
			trackChangedChecker++;
		}

		Track currentTrack = selectedTrack.get();
		long currentMicroseconds = currentTrack.getCurrentMicroseconds();
		double progress = (double) currentMicroseconds / longestTrackMicrosecondsLength;
		boolean currentTrackHasEnded = currentTrack.getTotalMicroseconds() == currentMicroseconds;

		/*
		 * Disable tracks with a length shorter than the current position. Also
		 * enables them again after resetting via stop.
		 */
		for (Toggle toggle : toggleGroupActiveTrack.getToggles()) {
			RadioButton radio = (RadioButton) toggle;
			Track track = (Track) radio.getUserData();
			radio.setDisable(track != currentTrack && currentMicroseconds > track.getTotalMicroseconds());
		}

		/*
		 * This seems to ensure that the actual update is done on the Java FX
		 * thread. Trying to update GUI components from another thrßead can lead
		 * to IllegalStateExceptions.
		 */
		Platform.runLater(() -> {
			progressBarTime.setProgress(progress);
			sliderProgressBarTime.setValue(currentMicroseconds - 250000);
			textCurrentTime.setText(formatTimeString(currentMicroseconds));
			if (currentTrackHasEnded) {
				buttonPlayPause.setText(ICON_PLAY);
			}
		});
	}

	private void handleAddTracks(ActionEvent event) {
		FileChooser chooser = new FileChooser();

		/*
		 * TODO: should restrict file types! but maybe don't hardcode, rather
		 * 'ask' a responsible class what file types are allowed?
		 */

		chooser.setTitle("TRANSLATE ME");
		List<File> files = emptyListIfNull(chooser.showOpenMultipleDialog(stage));
		for (File file : files) {
			addFile(file);
		}
	}

	public void addFile(File file) {
		Track track;
		try {
			track = TrackFactory.loadTrack(file.getAbsolutePath());
		} catch (UnsupportedFormatException e) {
			this.showErrorUnsupportedFormat(e.getFormat(), e.getAudioFormat());
			return;
		}

		if (checkMenuItemSyncronizeStartPoints.isSelected()) {
			track.applyStartPointOffset();
		}
		/*
		 * Needs to be added before drawing so that the longest track can be
		 * determined.
		 */
		project.addTrack(track);
		loadTrackUis(Arrays.asList(track));
	}

	private void loadTrackUis(List<Track> tracks) {
		determineLongestTrackLengths();

		for (Track track : tracks) {
			loadTrackUi(track);
		}
		setPlaybackControlsDisable(tracks.isEmpty());

		addButtons();
		project.setLoudnessLevel();
		// setMoveButtons();
		setButtonsEventHandler();
	}

	public void setPlaybackControlsDisable(boolean disable) {
		buttonPlayPause.setDisable(disable);
		buttonStop.setDisable(disable);
	}

	public void determineLongestTrackLengths() {
		if (project.getTracks().isEmpty()) {
			return;
		}
		Track longestTrack = project.getTracks().stream().max(comparing(Track::getTotalMicroseconds)).get();

		longestTrackFrameLength = longestTrack.getLength();
		longestTrackMicrosecondsLength = longestTrack.getTotalMicroseconds();

		trackControllers.forEach(controller -> controller.setLongestTrackFrameLength(longestTrackFrameLength));

		String timeString = formatTimeString(longestTrackMicrosecondsLength);
		textTotalTime.setText(timeString);

		// Set the Slider to the same length as the Progressbar
		sliderProgressBarTime.setMin(0);
		sliderProgressBarTime.setMax(longestTrackMicrosecondsLength - 250000);
	}

	private void loadTrackUi(Track track) {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setController(new TrackController(track, toggleGroupActiveTrack, longestTrackFrameLength));
			loader.setLocation(getClass().getClassLoader().getResource("views/Track.fxml"));
			loader.setResources(bundle);
			trackControllers.add(loader.getController());

			vboxTracks.getChildren().add(loader.load());
		} catch (IOException e) {
			throw new RuntimeException("Error while loading track UI.", e);
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

	private void handleStop(ActionEvent event) {
		getSelectedTrack().ifPresent(Track::stop);
		buttonPlayPause.setText(ICON_PLAY);
	}

	private void handleAbout(ActionEvent event) {
		LocalizedAlertBuilder builder = new LocalizedAlertBuilder(bundle, "about.", AlertType.CONFIRMATION);
		builder.setHeaderText(null);
		Alert alertAbout = builder.build();

		((Label) alertAbout.getDialogPane().getChildren().get(1)).setWrapText(false);
		alertAbout.getDialogPane().setPrefHeight(Region.USE_COMPUTED_SIZE);
		// TODO: auto-resize to content
		alertAbout.getDialogPane().setPrefWidth(700);

		alertAbout.showAndWait();
	}

	private void showErrorUnsupportedFormat(Format format, AudioFormat audioFormat) {
		LocalizedAlertBuilder builder = new LocalizedAlertBuilder(bundle, "errorUnsupportedFormat.", AlertType.ERROR);
		builder.setHeaderText(null);

		String errorText = bundle.getString(determineErrorDescriptionForFormat(format, audioFormat));
		errorText += bundle.getString("errorUnsupportedFormat.supportedFormats");
		builder.setContentText(errorText);
		Alert alertError = builder.build();

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

	private void setMoveButtons() {
		List<Track> tracks = project.getTracks();
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
		List<Track> tracks = project.getTracks();

		String trackName = tracks.get(number).getName();
		LocalizedAlertBuilder builder = new LocalizedAlertBuilder(bundle, "alert.deleteTrack.", AlertType.CONFIRMATION);
		builder.setHeaderFormatParameters(trackName);

		Optional<ButtonType> result = builder.build().showAndWait();
		if (result.get() == ButtonType.OK) {
			removeTrack(number);
			determineLongestTrackLengths();
		}
	}

	private void removeTrack(int number) {
		List<Track> tracks = project.getTracks();
		handleStop(null);
		vboxTracks.getChildren().remove(number);

		Track removed = tracks.remove(number);
		toggleGroupActiveTrack.getToggles().removeIf(toggle -> toggle.getUserData().equals(removed));

		trackControllers.remove(number);
		moveButtonList.remove(number);
		deleteButtonList.remove(number);

		addButtons();
		// setMoveButtons();
		setButtonsEventHandler();
		project.setLoudnessLevel();
		if (tracks.size() > 0) {
			trackControllers.get(0).getRadioButtonActiveTrack().fire();
		} else {
			setPlaybackControlsDisable(true);
		}
	}

	private void moveUp(int number) {
		// System.out.println("number: " + number);
		if (number != 0) {
			List<Node> tempVboxTracks = new ArrayList<>();
			List<TrackController> tempTrackController = new ArrayList<>();
			List<Track> tempTracks = new ArrayList<>();

			List<Track> tracks = project.getTracks();
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
		List<Track> tracks = project.getTracks();
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

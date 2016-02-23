package at.fhtw.mcs.controller;

import static at.fhtw.mcs.util.NullSafety.emptyListIfNull;
import static java.util.Comparator.comparing;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import org.controlsfx.control.RangeSlider;

import at.fhtw.mcs.model.Format;
import at.fhtw.mcs.model.Project;
import at.fhtw.mcs.model.Track;
import at.fhtw.mcs.ui.LocalizedAlertBuilder;
import at.fhtw.mcs.ui.ProgressOverlay;
import at.fhtw.mcs.util.AudioOuput;
import at.fhtw.mcs.util.TrackFactory;
import at.fhtw.mcs.util.TrackFactory.UnsupportedFormatException;
import at.fhtw.mcs.util.VersionCompare;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller class for Root.fxml
 */
public class RootController implements Initializable {

	/*
	 * Where to move those? ResourceBundles?
	 */
	private static final String ICON_PAUSE = "||";
	private static final String ICON_PLAY = "\u25B6";
	private static final String URL_MANUAL = "https://github.com/flpa/mcs/wiki";

	@FXML
	private StackPane stackPaneRoot;
	@FXML
	private VBox vboxTracks;
	@FXML
	private CheckMenuItem checkMenuItemSyncronizeStartPoints;
	@FXML
	private CheckMenuItem checkMenuItemLoopPlayback;
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
	private MenuItem menuItemManual;
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
	@FXML
	private RangeSlider rangesliderLoop;

	private ToggleGroup toggleGroupActiveTrack = new ToggleGroup();
	private ResourceBundle bundle;
	private Stage stage;
	private FileChooser fileChooser = new FileChooser();
	private ProgressOverlay progressOverlay;

	private List<TrackController> trackControllers = new ArrayList<>();
	private List<List<Button>> moveButtonList = new ArrayList<>();
	private List<Button> deleteButtonList = new ArrayList<>();
	private List<LineChart<Number, Number>> lineChartList = new ArrayList<>();
	private List<Canvas> canvasList = new ArrayList<>();
	private List<AnchorPane> anchorPaneTrackList = new ArrayList<>();
	private Map<Rectangle, String> trackComments = new HashMap<Rectangle, String>();
	private Map<Rectangle, List<Double>> trackCommentPositions = new HashMap<Rectangle, List<Double>>();
	private List<Rectangle> clickedComment = new ArrayList<Rectangle>();

	// TODO: could be a configuration parameter?
	private long updateFrequencyMs = 100;
	private int longestTrackFrameLength;
	private long longestTrackMicrosecondsLength;
	private Project project;
	private Boolean startOfProject = true;
	private Boolean loopActive = false;
	private Boolean cPressed = false;

	Boolean trackChanged = false;
	int trackChangedChecker = 0;

	public RootController(Stage stage) {
		this.stage = stage;

		stage.setOnCloseRequest(event -> {
			if (handleUnsavedChanges() == false) {
				event.consume();
			}
		});
	}

	@Override
	public void initialize(URL viewSource, ResourceBundle translations) {
		this.bundle = translations;
		FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
				bundle.getString("fileChooser.addTrack.filterText"), "*.mp3", "*.wav", "*.wave", "*.aif", "*.aiff");
		fileChooser.getExtensionFilters().add(filter);

		VersionCompare versionCompare = new VersionCompare(bundle);
		(new Thread(versionCompare)).start();

		startUpDialog();

		startOfProject = false;

		menuItemQuit.setOnAction(e -> afterUnsavedChangesAreHandledDo(Platform::exit));
		menuItemNewProject.setOnAction(e -> afterUnsavedChangesAreHandledDo(this::newProject));
		menuItemOpenProject.setOnAction(e -> afterUnsavedChangesAreHandledDo(this::openProject));
		menuItemSaveProject.setOnAction(e -> this.save());
		menuItemSaveProject.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
		menuItemSaveProjectAs.setOnAction(e -> this.saveAs());
		menuItemCloseProject.setOnAction(e -> afterUnsavedChangesAreHandledDo(this::closeProject));

		menuItemAddTracks.setOnAction(this::handleAddTracks);
		menuItemAbout.setOnAction(this::handleAbout);

		menuItemManual.setOnAction(this::handleManual);

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

		checkMenuItemLoopPlayback.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				toggleLoopActive();
				project.setLoopActivated(newValue);
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
					Track prevTrack = getToggleTrack(previousSelection);
					wasPlaying = prevTrack.isPlaying();

					prevTrack.pause();
					currentMs = prevTrack.getCurrentMicroseconds();
				}

				if (newSelection != null) {
					Track newTrack = getToggleTrack(newSelection);
					newTrack.setCurrentMicroseconds(currentMs);
					if (wasPlaying) {
						newTrack.play();
					}
				}

				setStylesheetsForTracks();
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
				for (Canvas canvas : canvasList) {
					drawOnCanvas(canvas);
				}
				if ((double) newValue - (double) oldValue > 2_500_000 || (double) newValue - (double) oldValue < 0) {
					if (!trackChanged) {
						for (Track track : project.getTracks()) {
							long temp = Math.round((double) newValue);
							track.setCurrentMicroseconds(temp + 250000);
						}
					}
				}
			}
		});

		progressOverlay = new ProgressOverlay(this.stackPaneRoot, bundle.getString("label.addTracks.progress"));

		/*
		 * Loopslider and Line initialisation
		 */
		rangesliderLoop.setLowValue(rangesliderLoop.getMin());
		rangesliderLoop.setHighValue(rangesliderLoop.getMax());
		toggleLoopActive();

		rangesliderLoop.lowValueProperty().addListener((observable, oldValue, newValue) -> {
			project.setLoopLowValue((double) newValue);
			for (Canvas canvas : canvasList) {
				drawOnCanvas(canvas);
			}
		});
		rangesliderLoop.highValueProperty().addListener((observable, oldValue, newValue) -> {
			project.setLoopHighValue((double) newValue);
			for (Canvas canvas : canvasList) {
				drawOnCanvas(canvas);
			}
		});

		stackPaneRoot.widthProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				for (AnchorPane anchorPane : anchorPaneTrackList) {
					List<Node> children = anchorPane.getChildren();
					for (Node child : children) {
						if (child instanceof Rectangle) {
							repositionComments((Rectangle) child, anchorPane);
						}
					}
				}
				for (Canvas canvas : canvasList) {
					canvas.setWidth(canvas.getWidth() - ((double) oldValue - (double) newValue));
				}
				for (AnchorPane anchorPane : anchorPaneTrackList) {
					anchorPane.setPrefWidth((double) newValue - 17);
				}
			}
		});

		stackPaneRoot.setOnKeyPressed(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent ke) {
				if (ke.getCode().equals(KeyCode.SHIFT)) {
					cPressed = true;
				} else if (ke.getCode().equals(KeyCode.BACK_SPACE)) {
					if (!clickedComment.isEmpty()) {
						for (AnchorPane anchorPane : anchorPaneTrackList) {
							anchorPane.getChildren().remove(clickedComment.get(0));
						}
					}
				}
			}
		});

		stackPaneRoot.setOnKeyReleased(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent ke) {
				if (ke.getText().equals("c")) {
					cPressed = false;
				}
			}
		});

		// adding the stylesheet for comment Rectangles
		stackPaneRoot.getStylesheets().add(getClass().getClassLoader().getResource("css/Comment.css").toExternalForm());
	}

	private void startUpDialog() {
		LocalizedAlertBuilder builder = new LocalizedAlertBuilder(bundle, "alert.OpenOrNew.", AlertType.CONFIRMATION);
		ButtonType newProject = new ButtonType(bundle.getString("alert.OpenOrNew.button.new"), ButtonData.YES);
		ButtonType openProject = new ButtonType(bundle.getString("alert.OpenOrNew.button.open"), ButtonData.OTHER);
		ButtonType closeProject = new ButtonType(bundle.getString("alert.OpenOrNew.button.cancel"), ButtonData.NO);
		builder.setButtons(closeProject, openProject, newProject);

		Alert alertOpenOrNew = builder.build();
		for (Node node : alertOpenOrNew.getDialogPane().getChildren()) {
			if (node instanceof ButtonBar) {
				((ButtonBar) node).setButtonOrder("NYU");
			}
		}

		Optional<ButtonType> result = alertOpenOrNew.showAndWait();

		if (!result.isPresent()) {
			Platform.exit();
		} else {
			if (result.get().equals(newProject)) {
				newProject();
			} else if (result.get().equals(openProject)) {
				openProject();
			} else {
				Platform.exit();
			}
		}
	}

	private void afterUnsavedChangesAreHandledDo(Runnable callback) {
		if (handleUnsavedChanges()) {
			callback.run();
		}
	}

	private boolean handleUnsavedChanges() {
		return project.hasUnsavedChanges() == false || letUserHandleUnsavedChanges();
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
		FileChooser fileChooser = new FileChooser();
		File f = fileChooser.showSaveDialog(stage);
		if (f != null) {
			project.setDirectory(f);
			save();
		} else if (startOfProject) {
			startUpDialog();
		}
	}

	private void setProject(Project project) {
		removeAllTracks();
		this.project = project;
		updateApplicationTitle();
		loadTrackUis(project.getTracks());
		sliderMasterVolume.setValue(project.getMasterLevel());
		checkMenuItemSyncronizeStartPoints.setSelected(project.isSynchronizeStartPoints());
		rangesliderLoop.setLowValue(project.getLoopLowValue());
		rangesliderLoop.setHighValue(project.getLoopHighValue());
		rangesliderLoop.setMax(project.getLoopMaxValue());
		rangesliderLoop.setMin(project.getLoopMinValue());
		checkMenuItemLoopPlayback.setSelected(project.isLoopActivated());
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
				openProject();
			}
		} else if (startOfProject) {
			startUpDialog();
		}
	}

	private void handleOpenError(Exception e) {
		e.printStackTrace();

		LocalizedAlertBuilder builder = new LocalizedAlertBuilder(bundle, "alert.loadFailed.", AlertType.ERROR);
		builder.setContentKey(e instanceof FileNotFoundException ? "content.loadError" : "content.parseError");
		builder.build().showAndWait();
	}

	private void closeProject() {
		startOfProject = true;
		startUpDialog();
	}

	private void updateApplicationTitle() {
		String format = MessageFormat.format(bundle.getString("app.title") + "{1}", getProjectName(),
				project.hasUnsavedChanges() ? "*" : "");
		Platform.runLater(() -> stage.setTitle(format));
	}

	private static boolean isOutputMixerInfo(Mixer.Info info) {
		return AudioSystem.getMixer(info).isLineSupported(new Line.Info(Clip.class));
	}

	private Optional<Track> getSelectedTrack() {
		Toggle selectedToggle = toggleGroupActiveTrack.getSelectedToggle();
		if (selectedToggle == null) {
			return Optional.empty();
		}
		return Optional.of(getToggleTrack(selectedToggle));
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
			Track track = getToggleTrack(toggle);
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
			if (loopActive) {
				if (sliderProgressBarTime.getValue() > rangesliderLoop.getHighValue()) {
					sliderProgressBarTime.setValue(rangesliderLoop.getLowValue());
				}
				if (sliderProgressBarTime.getValue() < rangesliderLoop.getLowValue()) {
					sliderProgressBarTime.setValue(rangesliderLoop.getLowValue());
				}
			}
			if (currentTrackHasEnded) {
				buttonPlayPause.setText(ICON_PLAY);
			}
		});
	}

	private void handleAddTracks(ActionEvent event) {
		List<File> files = emptyListIfNull(fileChooser.showOpenMultipleDialog(stage));
		addFiles(files);
		if (files.isEmpty() == false) {
			fileChooser.setInitialDirectory(files.get(files.size() - 1).getParentFile());
		}
	}

	public void addFiles(File... files) {
		addFiles(Arrays.asList(files));
	}

	public void addFiles(List<File> files) {
		if (files.isEmpty()) {
			return;
		}

		progressOverlay.show();

		new Thread(() -> {
			try {
				files.forEach(this::addFile);
			} finally {
				Platform.runLater(progressOverlay::hide);
			}
		}).start();
	}

	private void addFile(File file) {
		Track track;
		try {
			track = TrackFactory.loadTrack(file.getAbsolutePath(), project.getDirectory().toString());
		} catch (UnsupportedFormatException e) {
			Platform.runLater(() -> {
				this.showErrorUnsupportedFormat(e.getFormat(), e.getAudioFormat());
			});
			return;
		} catch (OutOfMemoryError e) {
			Platform.runLater(this::showErrorOutOfMemory);
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
		Platform.runLater(() -> {
			determineLongestTrackLengths();

			for (Track track : tracks) {
				loadTrackUi(track);
			}
			setPlaybackControlsDisable(tracks.isEmpty());

			addButtonsAndChart();
			project.setLoudnessLevel();
			// setMoveButtons();
			setButtonsEventHandler();
			setLineChartEventHandler();
			setStylesheetsForTracks();
			setCanvasEventHandler();
			for (Canvas canvas : canvasList) {
				drawOnCanvas(canvas);
			}
		});
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

		longestTrackFrameLength = longestTrack.getLengthWeighted();
		longestTrackMicrosecondsLength = longestTrack.getTotalMicroseconds();

		trackControllers.forEach(controller -> controller.setLongestTrackFrameLength(longestTrackFrameLength));
		String timeString = formatTimeString(longestTrackMicrosecondsLength);
		textTotalTime.setText(timeString);

		// Set the Slider to the same length as the Progressbar
		sliderProgressBarTime.setMin(0);
		sliderProgressBarTime.setMax(longestTrackMicrosecondsLength - 250000);
		rangesliderLoop.setMin(0);
		rangesliderLoop.setMax(longestTrackMicrosecondsLength - 250000);
		project.setLoopMinValue(0);
		project.setLoopMaxValue(longestTrackMicrosecondsLength - 250000);
		rangesliderLoop.setLowValue(project.getLoopLowValue());
		if (project.getLoopHighValue() == 0) {
			rangesliderLoop.setHighValue(longestTrackMicrosecondsLength - 250000);
		} else {
			rangesliderLoop.setHighValue(project.getLoopHighValue());
		}
	}

	private void loadTrackUi(Track track) {
		FXMLLoader loader = new FXMLLoader();
		loader.setController(new TrackController(track, toggleGroupActiveTrack, longestTrackFrameLength));
		loader.setLocation(getClass().getClassLoader().getResource("views/Track.fxml"));
		loader.setResources(bundle);
		trackControllers.add(loader.getController());
		try {
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
		LocalizedAlertBuilder builder = new LocalizedAlertBuilder(bundle, "about.", AlertType.INFORMATION);
		Alert alertAbout = builder.build();

		alertAbout.getDialogPane().setPrefHeight(Region.USE_COMPUTED_SIZE);
		// TODO: auto-resize to content
		alertAbout.getDialogPane().setPrefWidth(700);

		alertAbout.showAndWait();
	}

	private void handleManual(ActionEvent event) {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE)) {
			new Thread(() -> {
				try {
					Desktop.getDesktop().browse(new URI(URL_MANUAL));
				} catch (IOException | URISyntaxException e) {
					System.err.println("Error while opening manual webpage.");
					e.printStackTrace();
				}
			}).start();
		} else {
			LocalizedAlertBuilder builder = new LocalizedAlertBuilder(bundle, "manual.", AlertType.INFORMATION);
			String contentText = bundle.getString("manual.content");
			contentText += URL_MANUAL;
			builder.setContentText(contentText);
			Alert alertManual = builder.build();

			alertManual.getDialogPane().setPrefHeight(Region.USE_COMPUTED_SIZE);
			alertManual.getDialogPane().setPrefWidth(700);

			alertManual.showAndWait();
		}
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
		if (audioFormat == null) {
			return "errorUnsupportedFormat.contentDefault";
		}
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

	private void showErrorOutOfMemory() {
		LocalizedAlertBuilder builder = new LocalizedAlertBuilder(bundle, "errorOutOfMemory.", AlertType.ERROR);
		builder.setHeaderText(null);

		Alert alertError = builder.build();

		alertError.getDialogPane().setPrefHeight(Region.USE_COMPUTED_SIZE);
		alertError.getDialogPane().setPrefWidth(700);

		alertError.showAndWait();
	}

	private void addButtonsAndChart() {
		moveButtonList.clear();
		deleteButtonList.clear();
		lineChartList.clear();
		canvasList.clear();
		anchorPaneTrackList.clear();
		for (int i = 0; i < trackControllers.size(); i++) {
			// deleteButton
			deleteButtonList.add(trackControllers.get(i).getButtonDelete());

			// moveButtons
			List<Button> tempList = new ArrayList<>();
			tempList.add(trackControllers.get(i).getButtonMoveUp());
			tempList.add(trackControllers.get(i).getButtonMoveDown());
			moveButtonList.add(tempList);

			// Linechart Waveform
			lineChartList.add(trackControllers.get(i).getChart());

			// Canvas
			canvasList.add(trackControllers.get(i).getCanvas());

			// AnchorPane
			anchorPaneTrackList.add(trackControllers.get(i).getAnchorPane());
		}

		for (Canvas canvas : canvasList) {
			drawOnCanvas(canvas);
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

	private void setLineChartEventHandler() {
		for (int i = 0; i < lineChartList.size(); i++) {
			final int trackNumber = i;
			lineChartList.get(i).setOnMouseClicked(e -> {
				trackControllers.get(trackNumber).setRadioButtonActive();
				if (trackControllers.get(trackNumber).getRadioButtonActiveTrack().isSelected()) {
					trackControllers.get(trackNumber).getChart().getStylesheets()
							.add(getClass().getClassLoader().getResource("css/ActiveTrack.css").toExternalForm());
				}
			});
		}
	}

	private void setCanvasEventHandler() {
		for (int i = 0; i < canvasList.size(); i++) {
			final int trackNumber = i;
			canvasList.get(i).widthProperty().addListener(observable -> drawOnCanvas(canvasList.get(trackNumber)));
			canvasList.get(i).setOnMouseClicked(e -> {
				if (!cPressed) {
					trackControllers.get(trackNumber).setRadioButtonActive();
					if (trackControllers.get(trackNumber).getRadioButtonActiveTrack().isSelected()) {
						trackControllers.get(trackNumber).getChart().getStylesheets()
								.add(getClass().getClassLoader().getResource("css/ActiveTrack.css").toExternalForm());
					}
				} else {
					addCommentToCanvas(canvasList.get(trackNumber), e.getX(), e.getY(),
							anchorPaneTrackList.get(trackNumber));
				}
			});
		}
	}

	private void openAddCommentDialog(Canvas canvas, double x, double y, AnchorPane pane, Rectangle rect) {
		TextField comment = new TextField();
		comment.relocate((x + canvas.getLayoutX()), (y + canvas.getLayoutY()));
		pane.getChildren().add(comment);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				comment.requestFocus();
			}
		});

		comment.setOnAction((event) -> {
			Tooltip t = new Tooltip(comment.getText());
			trackComments.put(rect, comment.getText());
			shortenTooltipStartTiming(t);
			Tooltip.install(rect, t);
			pane.getChildren().remove(comment);

		});
	}

	private void addCommentToCanvas(Canvas canvas, double x, double y, AnchorPane pane) {

		Rectangle rect = new Rectangle(10, 10);
		rect.getStyleClass().add("comment");
		double xDelta = pane.getWidth();
		double relativxPos = ((x + canvas.getLayoutX()) / pane.getWidth());
		double yDelta = pane.getHeight();
		double relativyPos = ((y + canvas.getLayoutY()) / pane.getHeight());

		rect.relocate(relativxPos * xDelta, relativyPos * yDelta);
		pane.getChildren().add(rect);
		List<Double> tempPos = new ArrayList<Double>();
		tempPos.add(relativxPos);
		tempPos.add(relativyPos);
		trackCommentPositions.put(rect, tempPos);

		openAddCommentDialog(canvas, x, y, pane, rect);
		// TODO: add to Project

		// Comment eventhandler
		rect.hoverProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!clickedComment.isEmpty()) {
					if (clickedComment.get(0).equals(rect)) {
						return;
					}
				}
				if (newValue) {
					rect.getStyleClass().clear();
					rect.getStyleClass().add("hoveredComment");
				} else {
					rect.getStyleClass().clear();
					rect.getStyleClass().add("comment");
				}
			}
		});

		rect.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (!clickedComment.isEmpty()) {
					if (clickedComment.get(0).equals(rect)) {
						clickedComment.get(0).getStyleClass().clear();
						clickedComment.get(0).getStyleClass().add("hoveredComment");
						clickedComment.clear();
						return;
					} else {
						clickedComment.get(0).getStyleClass().clear();
						clickedComment.get(0).getStyleClass().add("comment");
						clickedComment.clear();
						clickedComment.add(rect);
						rect.getStyleClass().clear();
						rect.getStyleClass().add("clickedComment");
					}
				} else {
					clickedComment.add(rect);
					rect.getStyleClass().clear();
					rect.getStyleClass().add("clickedComment");
				}
			}

		});
	}

	private void repositionComments(Rectangle rect, AnchorPane pane) {
		double xDelta = pane.getWidth();
		double yDelta = pane.getHeight();

		rect.relocate(trackCommentPositions.get(rect).get(0) * xDelta, trackCommentPositions.get(rect).get(1) * yDelta);
	}

	public static void shortenTooltipStartTiming(Tooltip tooltip) {
		try {
			Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
			fieldBehavior.setAccessible(true);
			Object objBehavior = fieldBehavior.get(tooltip);

			Field fieldTimer = objBehavior.getClass().getDeclaredField("activationTimer");
			fieldTimer.setAccessible(true);
			Timeline objTimer = (Timeline) fieldTimer.get(objBehavior);

			objTimer.getKeyFrames().clear();
			objTimer.getKeyFrames().add(new KeyFrame(new Duration(70)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void drawOnCanvas(Canvas canvas) {
		if (loopActive) {
			drawProgressOnCanvas(canvas);
			double maxValueRangeSlider = rangesliderLoop.getMax();
			double rangeSliderLowValue = rangesliderLoop.getLowValue();
			double rangeSliderHighValue = rangesliderLoop.getHighValue();
			double canvasWidth = canvas.getWidth();
			double canvasDelta = maxValueRangeSlider / canvasWidth;
			double firstRectBorder = rangeSliderLowValue / canvasDelta;
			double secondRectBorder = rangeSliderHighValue / canvasDelta;

			GraphicsContext gc = canvas.getGraphicsContext2D();
			gc.setFill(Color.rgb(244, 244, 244));
			gc.setStroke(Color.rgb(244, 244, 244));
			gc.fillRect(0, 0, firstRectBorder, canvas.getHeight());
			gc.fillRect(secondRectBorder, 0, canvas.getWidth(), canvas.getHeight());
		} else {
			drawProgressOnCanvas(canvas);
		}
	}

	private void drawProgressOnCanvas(Canvas canvas) {
		clearCanvas(canvas);
		double maxValueProgress = sliderProgressBarTime.getMax();
		double progressValue = sliderProgressBarTime.getValue();
		double canvasWidth = canvas.getWidth();
		double canvasDelta = maxValueProgress / canvasWidth;
		double positionOfLine = progressValue / canvasDelta;

		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setFill(Color.BLACK);
		gc.setStroke(Color.BLACK);
		gc.fillRect(positionOfLine, 0, 1, canvas.getHeight());
	}

	private void clearCanvas(Canvas canvas) {
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
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
		toggleGroupActiveTrack.getToggles().removeIf(toggle -> removed.equals(getToggleTrack(toggle)));

		trackControllers.remove(number);
		moveButtonList.remove(number);
		deleteButtonList.remove(number);
		canvasList.remove(number);
		anchorPaneTrackList.remove(number);

		addButtonsAndChart();
		// setMoveButtons();
		setButtonsEventHandler();
		setCanvasEventHandler();
		setLineChartEventHandler();
		setStylesheetsForTracks();
		project.setLoudnessLevel();
		if (tracks.size() > 0) {
			trackControllers.get(0).getRadioButtonActiveTrack().fire();
		} else {
			setPlaybackControlsDisable(true);
		}
	}

	@SuppressWarnings("unchecked")
	private Track getToggleTrack(Toggle toggle) {
		return ((WeakReference<Track>) toggle.getUserData()).get();
	}

	private void moveUp(int number) {
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

			addButtonsAndChart();
			// setMoveButtons();
			setButtonsEventHandler();
			setLineChartEventHandler();
			setCanvasEventHandler();
			setStylesheetsForTracks();
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

			addButtonsAndChart();
			// setMoveButtons();
			setButtonsEventHandler();
			setLineChartEventHandler();
			setCanvasEventHandler();
			setStylesheetsForTracks();
		}
	}

	private void setStylesheetsForTracks() {
		for (TrackController trackController : trackControllers) {
			trackController.getChart().getStylesheets().clear();
			if (trackController.getRadioButtonActiveTrack().isSelected()) {
				trackController.getChart().getStylesheets()
						.add(getClass().getClassLoader().getResource("css/ActiveTrack.css").toExternalForm());

			} else {
				trackController.getChart().getStylesheets()
						.add(getClass().getClassLoader().getResource("css/NotActiveTrack.css").toExternalForm());
			}
		}
	}

	public void sceneInitialization(Scene scene) {
		scene.getAccelerators().put(new KeyCodeCombination(KeyCode.SPACE), buttonPlayPause::fire);

		// To unfocus the comment text field
		scene.setOnMouseClicked(event -> scene.getRoot().requestFocus());
	}

	private void toggleLoopActive() {
		loopActive = checkMenuItemLoopPlayback.isSelected();

		if (loopActive) {
			rangesliderLoop.setDisable(false);
			for (Canvas canvas : canvasList) {
				drawOnCanvas(canvas);
			}
		} else {
			rangesliderLoop.setDisable(true);
			for (Canvas canvas : canvasList) {
				clearCanvas(canvas);
			}
		}
	}
}

package at.fhtw.mcs.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.io.FilenameUtils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

@XmlRootElement
public class Project {
	private static JAXBContext jaxbContext = initializeJaxbContext();

	private BooleanProperty synchronizeStartPoints = new SimpleBooleanProperty(true);
	private DoubleProperty masterLevel = new SimpleDoubleProperty(1.0);
	private File directory;
	@XmlJavaTypeAdapter(XmlSaveableTrack.Adapter.class)
	@XmlElement(name = "track")
	private ObservableList<Track> tracks = FXCollections.observableArrayList();

	private BooleanProperty unsavedChanges = new SimpleBooleanProperty(false);

	public Project() {
		// Those listeners are only called when values have actually changed
		// TODO: tried a generic listener-lambda-function but that didn't work
		synchronizeStartPoints.addListener((observable, oldVal, newVal) -> unsavedChanges.set(true));
		masterLevel.addListener((observable, oldVal, newVal) -> unsavedChanges.set(true));
		tracks.addListener(this::handleTrackListChange);
	}

	private void handleTrackListChange(Change<? extends Track> change) {
		unsavedChanges.set(true);
		while (change.next()) {
			if (change.wasAdded()) {
				// register a comment change listener for each new track
				for (Track track : change.getAddedSubList()) {
					track.registerCommentChangeListener((observable, oldVal, newVal) -> unsavedChanges.set(true));
				}
			}
		}
	}

	private static JAXBContext initializeJaxbContext() {
		try {
			return JAXBContext.newInstance(Project.class);
		} catch (JAXBException e) {
			throw new IllegalStateException(
					"Project initialization failed: There was a problem creating the JAXB context, which means that it would not be possible to save or load projects.",
					e);
		}
	}

	public void save() throws IOException {
		directory.mkdir();
		OutputStream stream = new FileOutputStream(getProjectFile());
		try {
			Marshaller marshaller = createMarshaller();
			marshaller.marshal(this, stream);

			for (Track track : tracks) {
				track.saveAs(new File(directory, track.getFileName()));
			}

			unsavedChanges.set(false);
		} catch (JAXBException e) {
			throw new IllegalStateException(
					"Error while creating the project XML file. This is most likely a bug and means that "
							+ "the data model of the application can no longer be converted to XML.",
					e);
		}
	}

	private File getProjectFile() {
		return getProjectFile(directory);
	}

	private static File getProjectFile(File directory) {
		return new File(directory, "mcs-project.xml");
	}

	private Marshaller createMarshaller() throws JAXBException, PropertyException {
		Marshaller m = jaxbContext.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		return m;
	}

	/**
	 * Loads a project from a given directory.
	 * 
	 * @param directory
	 * @return
	 * @throws FileNotFoundException
	 *             In case the project file does not exist or is not a valid
	 *             file (e.g. a directory instead)
	 * @throws JAXBException
	 *             In case there is an error while parsing the project file.
	 */
	public static Project load(File directory) throws FileNotFoundException, JAXBException {
		FileInputStream inputStream = new FileInputStream(getProjectFile(directory));
		Unmarshaller unmarshaller = createUnmarshaller(directory);

		Project project = (Project) unmarshaller.unmarshal(inputStream);
		project.setDirectory(directory);
		if (project.isSynchronizeStartPoints()) {
			project.tracks.forEach(Track::applyStartPointOffset);
		}
		project.unsavedChanges.set(false);

		return project;
	}

	private static Unmarshaller createUnmarshaller(File directory) throws JAXBException {
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		/*
		 * Manually set the track adapter so that it knows the directory we're
		 * loading from.
		 */
		XmlSaveableTrack.Adapter adapter = new XmlSaveableTrack.Adapter();
		adapter.setDirectory(directory);
		unmarshaller.setAdapter(adapter);

		/*
		 * TODO: should probably set a schema so that the Unmarshaller can
		 * validate.
		 */

		return unmarshaller;
	}

	@XmlTransient
	public String getName() {
		return directory == null ? null : FilenameUtils.getBaseName(directory.getPath());
	}

	@XmlAttribute
	public double getMasterLevel() {
		return masterLevel.get();
	}

	public void setMasterLevel(double masterLevel) {
		this.masterLevel.set(masterLevel);
		for (Track track : tracks) {
			track.changeVolume(masterLevel);
		}
	}

	@XmlAttribute
	public boolean isSynchronizeStartPoints() {
		return synchronizeStartPoints.get();
	}

	public void setSynchronizeStartPoints(boolean synchronizeStartPoints) {
		this.synchronizeStartPoints.set(synchronizeStartPoints);

		for (Track track : tracks) {
			if (synchronizeStartPoints) {
				track.applyStartPointOffset();
			} else {
				track.resetStartPointOffset();
			}
		}
	}

	@XmlTransient
	public File getDirectory() {
		return directory;
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}

	public void addTrack(Track track) {
		List<String> existingNames = tracks.stream().map(Track::getName).collect(Collectors.toList());

		String originalName = track.getName();
		int i = 2;
		String name = originalName;

		while (existingNames.contains(name)) {
			name = String.format("%s(%d)", originalName, i);
			i++;
		}

		track.setName(name);
		tracks.add(track);
	}

	public List<Track> getTracks() {
		return tracks;
	}

	public void setLoudnessLevel() {
		float min = 0;

		for (Track trackiterator : tracks) {
			if (min > trackiterator.getLoudness()) {
				min = trackiterator.getLoudness();
			}
		}

		for (Track track : tracks) {
			track.setVolume(min);
			track.changeVolume(getMasterLevel());
		}
	}

	public boolean hasUnsavedChanges() {
		return unsavedChanges.get();
	}

	public BooleanProperty unsavedChangesProperty() {
		return unsavedChanges;
	}
}

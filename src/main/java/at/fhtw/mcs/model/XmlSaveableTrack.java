package at.fhtw.mcs.model;

import java.io.File;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.io.FilenameUtils;

import at.fhtw.mcs.util.TrackFactory;

public class XmlSaveableTrack {

	@XmlAttribute
	private String name;
	@XmlElement
	private String comment;

	public XmlSaveableTrack() {
	}

	public XmlSaveableTrack(String name, String comment) {
		this.name = name;
		this.comment = comment;
	}

	static class Adapter extends XmlAdapter<XmlSaveableTrack, Track> {
		private File directory;

		public void setDirectory(File directory) {
			this.directory = directory;
		}

		public Track unmarshal(XmlSaveableTrack xmlTrack) {
			File file = findFileInDirectory(xmlTrack.name);
			Track track = TrackFactory.loadTrack(file.getAbsolutePath(), directory.toString());

			track.setComment(xmlTrack.comment);
			return track;
		}

		private File findFileInDirectory(String baseName) {
			File[] matchingFiles = directory
					.listFiles(file -> baseName.equals(FilenameUtils.getBaseName(file.getName())));

			if (matchingFiles.length == 0) {
				throw new IllegalArgumentException(String.format("No file with base name '%s' in directory '%s'!",
						baseName, directory));
			}
			if (matchingFiles.length > 1) {
				throw new IllegalArgumentException(String.format(
						"Multiple files with base name '%s' in directory '%s'!", baseName, directory));
			}

			return matchingFiles[0];
		}

		public XmlSaveableTrack marshal(Track track) {
			return new XmlSaveableTrack(track.getName(), track.getComment());
		}
	}
}

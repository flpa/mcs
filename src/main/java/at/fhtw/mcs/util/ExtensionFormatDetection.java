package at.fhtw.mcs.util;

import org.apache.commons.io.FilenameUtils;

import at.fhtw.mcs.model.Format;

public class ExtensionFormatDetection implements FormatDetection {

	@Override
	public Format detectFormat(String path) {
		switch (FilenameUtils.getExtension(path).toLowerCase()) {
			case "mp3":
				return Format.MP3;
			case "wav":
			case "wave":
				return Format.WAV;
			case "aiff":
			case "aif":
				return Format.AIFF;
			default:
				return Format.UNKNOWN;
		}
	}
}

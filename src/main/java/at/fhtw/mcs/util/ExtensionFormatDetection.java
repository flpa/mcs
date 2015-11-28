package at.fhtw.mcs.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

	@Override
	public Format detectMimeType(String path) {
		Path pathFromString = Paths.get(path);

		try {
			String mimeType = Files.probeContentType(pathFromString);
			switch (mimeType) {
				case "audio/aiff":
				case "audio/x-aiff":
					return Format.AIFF;
				case "audio/wav":
				case "audio/x-wav":
					return Format.WAV;
				case "audio/mpeg3":
				case "audio/x-mpeg-3":
					return Format.MP3;
				default:
					return Format.UNKNOWN;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

}

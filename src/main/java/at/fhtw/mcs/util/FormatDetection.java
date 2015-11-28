package at.fhtw.mcs.util;

import at.fhtw.mcs.model.Format;

public interface FormatDetection {
	Format detectFormat(String path);

	Format detectMimeType(String path);
}

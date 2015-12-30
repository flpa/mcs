package at.fhtw.mcs.util;

import java.util.Collections;
import java.util.List;

public class NullSafety {
	public static <T> List<T> emptyListIfNull(List<T> list) {
		return list == null ? Collections.emptyList() : list;
	}
}

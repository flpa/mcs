package at.fhtw.mcs.model;

public class Version implements Comparable<Version> {
	private Integer major;
	private Integer minor;
	private Integer hotfix;

	public Version(String version) {
		version = version.replaceAll("[^0-9.]", "");
		String[] parts = version.split("[.]");
		this.major = Integer.parseInt(parts[0]);
		this.minor = Integer.parseInt(parts[1]);
		this.hotfix = Integer.parseInt(parts[2]);
	}

	@Override
	public int compareTo(Version other) {
		int i = this.major.compareTo(other.major);
		if (i != 0) {
			return i;
		}
		int j = this.minor.compareTo(other.minor);
		if (j != 0) {
			return j;
		}
		return Integer.compare(this.hotfix, other.hotfix);
	}

}

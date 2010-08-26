package au.org.ala.sensitiveData.model;

public class SensitiveSpecies implements Comparable<SensitiveSpecies> {

	private String scientificName;
	private SensitivityCategory sensitivityCategory;
	
	public SensitiveSpecies(String scientificName, SensitivityCategory sensitivityCategory) {
		super();
		this.scientificName = scientificName;
		this.sensitivityCategory = sensitivityCategory;
	}

	public String getScientificName() {
		return this.scientificName;
	}

	public SensitivityCategory getSensitivityCategory() {
		return sensitivityCategory;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((scientificName == null) ? 0 : scientificName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SensitiveSpecies other = (SensitiveSpecies) obj;
		if (scientificName == null) {
			if (other.scientificName != null)
				return false;
		} else if (!scientificName.equals(other.scientificName))
			return false;
		return true;
	}

	@Override
	public int compareTo(SensitiveSpecies ss) {
		return scientificName.compareTo(ss.getScientificName());
	}


}

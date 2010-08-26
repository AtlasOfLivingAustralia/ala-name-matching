package au.org.ala.sensitiveData.model;

public enum SensitivityCategory {

	EXTREME(1, -1, Integer.MAX_VALUE),
	HIGH(2, 1, 10000),
	MEDIUM(3, 2, 1000),
	LOW(4, 3, 100),
	NOT_SENSITIVE(5, 10, 0);
	
	private int value;
	private int generalisationDecimalPlaces;
	private int generalisationInMetres;
	
	private SensitivityCategory(int value, int decimalPlaces, int metres) {
		this.value = value;
		this.generalisationDecimalPlaces = decimalPlaces;
		this.generalisationInMetres = metres;
	}
	
	public static SensitivityCategory getCategory(int value) {
		for (SensitivityCategory cat : SensitivityCategory.values()) {
			if (cat.getValue() == value) {
				return cat;
			}
		}
		return null;
	}
	
	public int getValue() {
		return value;
	}

	public int getGeneralisationDecimalPlaces() {
		return generalisationDecimalPlaces;
	}

	public int getGeneralisationInMetres() {
		return generalisationInMetres;
	}
}

package au.org.ala.sensitiveData.model;

public class RawOccurrenceRecord {
	private int id;
	private String scientificName;
	private String latitude;
	private String longitude;
	private String latLongPrecision;
	
	public RawOccurrenceRecord() {
		super();
		// TODO Auto-generated constructor stub
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getScientificName() {
		return scientificName;
	}

	public void setScientificName(String scientificName) {
		this.scientificName = scientificName;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getLatLongPrecision() {
		return latLongPrecision;
	}

	public void setLatLongPrecision(String latLongPrecision) {
		this.latLongPrecision = latLongPrecision;
	}
	
	
}

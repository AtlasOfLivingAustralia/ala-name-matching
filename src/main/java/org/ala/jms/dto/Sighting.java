package org.ala.jms.dto;

/**
 * A DTO for a sighting.
 */
public class Sighting {

    /** the uid for the data resource supplying the sighting e.g. dr364*/
    String dataResourceUid;
    /** the id of the sighting */
    String id;
    /** guid for sighting */
    String guid;
	/** the id of the user who contributed sighting */
    String userId;
	/** the name of the user who contributed sighting */
    String collectorName;
    /** the guid of the taxon contributed sighting */
    String taxonConceptGuid;
    /** the sci name of the taxon contributed sighting */
    String scientificName;
    /** the rank of the taxon contributed sighting */
    String rank;
    /** the common name of the taxon contributed sighting */
    String vernacularName;
    /** the family of the taxon contributed sighting */
    String family;
    /** the kingdom of the taxon contributed sighting */
    String kingdom;
    /** the date of the observation in yyyy-MM-dd format*/
    String eventDate;
    /** the recorded time in hh:mm format */
    String eventTime;
    /** the number of individuals */
    Integer individualCount;
    /** the location as a string */
    String locality;
    /** the location as a string */
    String stateProvince;
    /** the location as a string */
    String geodeticDatum;
    /** latitude */
    Float latitude;
    /** longitude */
    Float longitude;
    /** Georeferencing method */
    String georeferenceProtocol;
    /** Confidence in the identification */
    String identificationVerificationStatus;
    /** coordinate uncertainty */
    Number coordinateUncertaintyInMeters;
    /** general notes */
    String country;
    /** general notes */
    String countryCode;
    /** general notes */
    String occurrenceRemarks;
    /** Images etc */
    String[] associatedMedia;
    /** Licence for the images */
    String licence;
    /** Rights for the images */
    String rights;
   
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
    /**
     * @return the guid
     */
	public String getGuid() {
		return guid;
	}
    /**
     * @param guid
     */
	public void setGuid(String guid) {
		this.guid = guid;
	}
	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	/**
	 * @return the userId
	 */
	public String getUserID() {
		return userId;
	}
	/**
	 * @param userId the userId to set
	 */
	public void setUserID(String userId) {
		this.userId = userId;
	}	
	/**
	 * @return the taxonConceptGuid
	 */
	public String getTaxonConceptGuid() {
		return taxonConceptGuid;
	}
	/**
	 * @param taxonConceptGuid the taxonConceptGuid to set
	 */
	public void setTaxonConceptGuid(String taxonConceptGuid) {
		this.taxonConceptGuid = taxonConceptGuid;
	}
	/**
	 * @return the scientificName
	 */
	public String getScientificName() {
		return scientificName;
	}
	/**
	 * @param scientificName the scientificName to set
	 */
	public void setScientificName(String scientificName) {
		this.scientificName = scientificName;
	}
	/**
	 * @return the family
	 */
	public String getFamily() {
		return family;
	}
	/**
	 * @param family the family to set
	 */
	public void setFamily(String family) {
		this.family = family;
	}
	/**
	 * @return the kingdom
	 */
	public String getKingdom() {
		return kingdom;
	}
	/**
	 * @param kingdom the kingdom to set
	 */
	public void setKingdom(String kingdom) {
		this.kingdom = kingdom;
	}
	/**
	 * @return the eventDate
	 */
	public String getEventDate() {
		return eventDate;
	}
	/**
	 * @param eventDate the eventDate to set
	 */
	public void setEventDate(String eventDate) {
		this.eventDate = eventDate;
	}
	/**
	 * @return the eventTime
	 */
	public String getEventTime() {
		return eventTime;
	}
	/**
	 * @param eventTime the eventTime to set
	 */
	public void setEventTime(String eventTime) {
		this.eventTime = eventTime;
	}
	/**
	 * @return the individualCount
	 */
	public Integer getIndividualCount() {
		return individualCount;
	}
	/**
	 * @param individualCount the individualCount to set
	 */
	public void setIndividualCount(Integer individualCount) {
		this.individualCount = individualCount;
	}
	/**
	 * @return the locality
	 */
	public String getLocality() {
		return locality;
	}
	/**
	 * @param locality the locality to set
	 */
	public void setLocality(String locality) {
		this.locality = locality;
	}
	/**
	 * @return the coordinateUncertaintyInMeters
	 */
	public Number getCoordinateUncertaintyInMeters() {
		return coordinateUncertaintyInMeters;
	}
	/**
	 * @param coordinateUncertaintyInMeters the coordinateUncertaintyInMeters to set
	 */
	public void setCoordinateUncertaintyInMeters(
			Number coordinateUncertaintyInMeters) {
		this.coordinateUncertaintyInMeters = coordinateUncertaintyInMeters;
	}
	/**
	 * @return the rank
	 */
	public String getRank() {
		return rank;
	}
	/**
	 * @param rank the rank to set
	 */
	public void setRank(String rank) {
		this.rank = rank;
	}
	/**
	 * @return the vernacularName
	 */
	public String getVernacularName() {
		return vernacularName;
	}
	/**
	 * @param vernacularName the vernacularName to set
	 */
	public void setVernacularName(String vernacularName) {
		this.vernacularName = vernacularName;
	}
	/**
	 * @return the collectorName
	 */
	public String getCollectorName() {
		return collectorName;
	}
	/**
	 * @param collectorName the collectorName to set
	 */
	public void setCollectorName(String collectorName) {
		this.collectorName = collectorName;
	}
	/**
	 * @return the latitude
	 */
	public Float getLatitude() {
		return latitude;
	}
	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(Float latitude) {
		this.latitude = latitude;
	}
	/**
	 * @return the latitude
	 */
	public Float getDecimalLatitude() {
		return latitude;
	}
	/**
	 * @param latitude the latitude to set
	 */
	public void setDecimalLatitude(Float latitude) {
		this.latitude = latitude;
	}
	/**
	 * @return the longitude
	 */
	public Float getDecimalLongitude() {
		return longitude;
	}
	/**
	 * @param longitude the longitude to set
	 */
	public void setDecimalLongitude(Float longitude) {
		this.longitude = longitude;
	}
	/**
	 * @return the longitude
	 */
	public Float getLongitude() {
		return longitude;
	}
	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(Float longitude) {
		this.longitude = longitude;
	}
	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}
	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}
	/**
	 * @return the countryCode
	 */
	public String getCountryCode() {
		return countryCode;
	}
	/**
	 * @param countryCode the countryCode to set
	 */
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	/**
	 * @return the occurrenceRemarks
	 */
	public String getOccurrenceRemarks() {
		return occurrenceRemarks;
	}
	/**
	 * @param occurrenceRemarks the occurrenceRemarks to set
	 */
	public void setOccurrenceRemarks(String occurrenceRemarks) {
		this.occurrenceRemarks = occurrenceRemarks;
	}
	/**
	 * @return the stateProvince
	 */
	public String getStateProvince() {
		return stateProvince;
	}
	/**
	 * @param stateProvince the stateProvince to set
	 */
	public void setStateProvince(String stateProvince) {
		this.stateProvince = stateProvince;
	}

	public String[] getAssociatedMedia() {
		return this.associatedMedia;
	}

	public void setAssociatedMedia(String[] associatedMedia) {
		this.associatedMedia = associatedMedia;
	}

    public String getGeodeticDatum() {
        return geodeticDatum;
    }

    public void setGeodeticDatum(String geodeticDatum) {
        this.geodeticDatum = geodeticDatum;
    }

    public String getDataResourceUid() {
        return dataResourceUid;
    }

    public void setDataResourceUid(String dataResourceUid) {
        this.dataResourceUid = dataResourceUid;
    }
    public String getLicence() {
        return licence;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    public String getRights() {
        return rights;
    }

    public void setRights(String rights) {
        this.rights = rights;
    }

    public String getGeoreferenceProtocol() {
        return georeferenceProtocol;
    }

    public void setGeoreferenceProtocol(String georeferenceProtocol) {
        this.georeferenceProtocol = georeferenceProtocol;
    }

    public String getIdentificationVerificationStatus() {
        return identificationVerificationStatus;
    }

    public void setIdentificationVerificationStatus(String identificationVerificationStatus) {
        this.identificationVerificationStatus = identificationVerificationStatus;
    }
}
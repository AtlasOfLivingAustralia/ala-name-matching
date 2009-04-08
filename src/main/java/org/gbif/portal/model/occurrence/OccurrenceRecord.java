/***************************************************************************
 * Copyright (C) 2009 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ***************************************************************************/
package org.gbif.portal.model.occurrence;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;

import org.gbif.portal.model.BaseObject;
import org.gbif.portal.model.geospatial.Country;
import org.gbif.portal.model.geospatial.GeoMapping;
import org.gbif.portal.model.resources.DataProvider;
import org.gbif.portal.model.resources.DataResource;
import org.gbif.portal.model.taxonomy.TaxonConcept;
import org.gbif.portal.model.taxonomy.TaxonConceptLite;
import org.gbif.portal.model.taxonomy.TaxonName;

/**
 * OccurrenceRecord Model Object represents a parsed Occurrence Record. 
 *
 * Note: This has been copied across form the GBIF code base so additional properties
 * can be added. 
 *
 * @author trobertson
 * @author dmartin
 */
public class OccurrenceRecord extends BaseObject {

	/** The provider of the record*/
	protected DataProvider dataProvider;
	/** The resource within the provider that contains this record */
	protected DataResource dataResource;
	/** The provider of the record*/
	protected Long dataProviderId;
	/** The resource within the provider that contains this record */
	protected Long dataResourceId;	
	/**  Institution code*/
	protected IdentifierCode institutionCode;
	/** Collection code */
	protected IdentifierCode collectionCode;
	/** Catalogue Number */
	protected IdentifierCode catalogueNumber;
	/** Concept within the taxonomy */
	protected Long taxonConceptId;
	/** Concept within the taxonomy */
	protected TaxonConcept taxonConcept;
	/** The taxon name within the local taxonomy */
	protected TaxonName taxonName;
	/** The taxon name within the local taxonomy */
	protected Long taxonNameId;
	/** Nub taxonomy concept id */
	protected Long nubTaxonConceptId;	
	/** Nub taxonomy concept if it exists */
	protected TaxonConcept nubTaxonConcept;
	/** The kingdom concept for this concept */
	protected TaxonConceptLite kingdomConcept;
	/** The kingdom concept for this concept */
	protected Long kingdomConceptId;
	/** The phylum concept for this concept */
	protected TaxonConceptLite phylumConcept;
	/** The phylum concept for this concept */
	protected Long phylumConceptId;
	/** The class concept for this concept */
	protected TaxonConceptLite classConcept;
	/** The class concept for this concept */
	protected Long classConceptId;
	/** The order concept for this concept */
	protected TaxonConceptLite orderConcept;
	/** The order concept for this concept */
	protected Long orderConceptId;
	/** The family concept for this concept */
	protected TaxonConceptLite familyConcept;
	/** The family concept for this concept */
	protected Long familyConceptId;
	/** The genus concept for this concept */
	protected TaxonConceptLite genusConcept;
	/** The genus concept for this concept */
	protected Long genusConceptId;
	/** The species concept for this concept */
	protected TaxonConceptLite speciesConcept;
	/** The species concept for this concept */
	protected Long speciesConceptId;
	/** Country code of the occurrence location */
	protected String isoCountryCode;
	/** The Country of the occurrence location */
	protected Country country;
	/** Latitude of occurrence */
	protected Float latitude;
	/** Longitude of occurrence */
	protected Float longitude;
	/** Altitude in metres */
	protected Integer altitudeInMetres;
	/** Depth in metres */
	protected Float depthInCentimetres;
	/** One degree cell id */
	protected Integer cellId;
	/** One-tenth degree cell id */
	protected Integer centiCellId;
	/** One-tenth degree cell id */
	protected Integer tenmilliCellId;
	/** The cell id mod 360 - used for bounding box calcs */
	protected Integer mod360CellId;
	/** The year of occurrence */
	protected Integer year;
	/** The month of occurrence */
	protected Integer month;
	/** The date of occurrence */
	protected Date occurrenceDate;
	/** The date of occurrence was deleted*/
	protected Timestamp modified;
	/** The date of occurrence was deleted*/
	protected Timestamp deleted;
	/** The basis of record */
	protected BasisOfRecord basisOfRecord;
	/** The basis of record */
	protected Integer basisOfRecordCode;
	/** Array of issues with taxonomic data */
	protected Integer taxonomicIssue;
	/** Array of issues with geospatial data */
	protected Integer geospatialIssue;
	/** Array of issues with other data */
	protected Integer otherIssue;
	/** the raw occurrence record */
	protected RawOccurrenceRecord rawOccurrenceRecord;
	/** The type status */
	protected Set<TypeStatus> typeStatus;
	/** The image associated with this record */
	protected Set<ORImage> orImage;
	/** The image associated with this record */
	protected Set<IdentifierRecord> identifierRecords;
	/** The geo mappings associated with this record */
	protected Set<GeoMapping> geoMappings;

	/**
	 * @return the dataProvider
	 */
	public DataProvider getDataProvider() {
		return dataProvider;
	}

	/**
	 * @param dataProvider the dataProvider to set
	 */
	public void setDataProvider(DataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}

	/**
	 * @return the dataResource
	 */
	public DataResource getDataResource() {
		return dataResource;
	}

	/**
	 * @param dataResource the dataResource to set
	 */
	public void setDataResource(DataResource dataResource) {
		this.dataResource = dataResource;
	}

	/**
	 * @return the dataProviderId
	 */
	public Long getDataProviderId() {
		return dataProviderId;
	}

	/**
	 * @param dataProviderId the dataProviderId to set
	 */
	public void setDataProviderId(Long dataProviderId) {
		this.dataProviderId = dataProviderId;
	}

	/**
	 * @return the dataResourceId
	 */
	public Long getDataResourceId() {
		return dataResourceId;
	}

	/**
	 * @param dataResourceId the dataResourceId to set
	 */
	public void setDataResourceId(Long dataResourceId) {
		this.dataResourceId = dataResourceId;
	}

	/**
	 * @return the isoCountryCode
	 */
	public String getIsoCountryCode() {
		return isoCountryCode;
	}

	/**
	 * @param isoCountryCode the isoCountryCode to set
	 */
	public void setIsoCountryCode(String isoCountryCode) {
		this.isoCountryCode = isoCountryCode;
	}
	
	/**
	 * @return the country
	 */
	public Country getCountry() {
		return country;
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(Country country) {
		this.country = country;
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
	 * @return the nubTaxonConcept
	 */
	public TaxonConcept getNubTaxonConcept() {
		return nubTaxonConcept;
	}

	/**
	 * @param nubTaxonConcept the nubTaxonConcept to set
	 */
	public void setNubTaxonConcept(TaxonConcept nubTaxonConcept) {
		this.nubTaxonConcept = nubTaxonConcept;
	}

	/**
	 * @return the nubTaxonConceptId
	 */
	public Long getNubTaxonConceptId() {
		return nubTaxonConceptId;
	}

	/**
	 * @param nubTaxonConceptId the nubTaxonConceptId to set
	 */
	public void setNubTaxonConceptId(Long nubTaxonConceptId) {
		this.nubTaxonConceptId = nubTaxonConceptId;
	}

	/**
	 * @return the taxonConcept
	 */
	public TaxonConcept getTaxonConcept() {
		return taxonConcept;
	}

	/**
	 * @param taxonConcept the taxonConcept to set
	 */
	public void setTaxonConcept(TaxonConcept taxonConcept) {
		this.taxonConcept = taxonConcept;
	}

	/**
	 * @return the taxonName
	 */
	public TaxonName getTaxonName() {
		return taxonName;
	}

	/**
	 * @param taxonName the taxonName to set
	 */
	public void setTaxonName(TaxonName taxonName) {
		this.taxonName = taxonName;
	}

	/**
	 * @return the taxonNameId
	 */
	public Long getTaxonNameId() {
		return taxonNameId;
	}

	/**
	 * @param taxonNameId the taxonNameId to set
	 */
	public void setTaxonNameId(Long taxonNameId) {
		this.taxonNameId = taxonNameId;
	}
	
	/**
	 * @return the cellId
	 */
	public Integer getCellId() {
		return cellId;
	}

	/**
	 * @param cellId the cellId to set
	 */
	public void setCellId(Integer cellId) {
		this.cellId = cellId;
	}

	/**
	 * @return the centiCellId
	 */
	public Integer getCentiCellId() {
		return centiCellId;
	}

	/**
	 * @param centiCellId the centiCellId to set
	 */
	public void setCentiCellId(Integer centiCellId) {
		this.centiCellId = centiCellId;
	}

	/**
	 * @return the geospatialIssue
	 */
	public Integer getGeospatialIssue() {
		return geospatialIssue;
	}

	/**
	 * @param geospatialIssue the geospatialIssue to set
	 */
	public void setGeospatialIssue(Integer geospatialIssue) {
		this.geospatialIssue = geospatialIssue;
	}

	/**
	 * @return the month
	 */
	public Integer getMonth() {
		return month;
	}

	/**
	 * @param month the month to set
	 */
	public void setMonth(Integer month) {
		this.month = month;
	}

	/**
	 * @return the occurrenceDate
	 */
	public Date getOccurrenceDate() {
		return occurrenceDate;
	}

	/**
	 * @param occurrenceDate the occurrenceDate to set
	 */
	public void setOccurrenceDate(Date occurrenceDate) {
		this.occurrenceDate = occurrenceDate;
	}

	/**
	 * @return the otherIssue
	 */
	public Integer getOtherIssue() {
		return otherIssue;
	}

	/**
	 * @param otherIssue the otherIssue to set
	 */
	public void setOtherIssue(Integer otherIssue) {
		this.otherIssue = otherIssue;
	}

	/**
	 * @return the taxonomicIssue
	 */
	public Integer getTaxonomicIssue() {
		return taxonomicIssue;
	}

	/**
	 * @param taxonomicIssue the taxonomicIssue to set
	 */
	public void setTaxonomicIssue(Integer taxonomicIssue) {
		this.taxonomicIssue = taxonomicIssue;
	}

	/**
	 * @return the year
	 */
	public Integer getYear() {
		return year;
	}

	/**
	 * @param year the year to set
	 */
	public void setYear(Integer year) {
		this.year = year;
	}

	/**
	 * @return the basisOfRecord
	 */
	public BasisOfRecord getBasisOfRecord() {
		return basisOfRecord;
	}

	/**
	 * @param basisOfRecord the basisOfRecord to set
	 */
	public void setBasisOfRecord(BasisOfRecord basisOfRecord) {
		this.basisOfRecord = basisOfRecord;
	}

	/**
	 * @return the basisOfRecordCode
	 */
	public Integer getBasisOfRecordCode() {
		return basisOfRecordCode;
	}

	/**
	 * @param basisOfRecordCode the basisOfRecordCode to set
	 */
	public void setBasisOfRecordCode(Integer basisOfRecordCode) {
		this.basisOfRecordCode = basisOfRecordCode;
	}

	/**
	 * @return the taxonConceptId
	 */
	public Long getTaxonConceptId() {
		return taxonConceptId;
	}

	/**
	 * @param taxonConceptId the taxonConceptId to set
	 */
	public void setTaxonConceptId(Long taxonConceptId) {
		this.taxonConceptId = taxonConceptId;
	}

	/**
	 * @return the orImage
	 */
	public Set<ORImage> getOrImage() {
		return orImage;
	}

	/**
	 * @param orImage the orImage to set
	 */
	public void setOrImage(Set<ORImage> orImage) {
		this.orImage = orImage;
	}

	/**
	 * @return the typeStatus
	 */
	public Set<TypeStatus> getTypeStatus() {
		return typeStatus;
	}

	/**
	 * @param typeStatus the typeStatus to set
	 */
	public void setTypeStatus(Set<TypeStatus> typeStatus) {
		this.typeStatus = typeStatus;
	}

	/**
	 * @return the identifierRecords
	 */
	public Set<IdentifierRecord> getIdentifierRecords() {
		return identifierRecords;
	}

	/**
	 * @param identifierRecords the identifierRecords to set
	 */
	public void setIdentifierRecords(Set<IdentifierRecord> identifierRecords) {
		this.identifierRecords = identifierRecords;
	}

	/**
	 * @return the rawOccurrenceRecord
	 */
	public RawOccurrenceRecord getRawOccurrenceRecord() {
		return rawOccurrenceRecord;
	}

	/**
	 * @param rawOccurrenceRecord the rawOccurrenceRecord to set
	 */
	public void setRawOccurrenceRecord(RawOccurrenceRecord rawOccurrenceRecord) {
		this.rawOccurrenceRecord = rawOccurrenceRecord;
	}

	/**
	 * @return the kingdomConcept
	 */
	public TaxonConceptLite getKingdomConcept() {
		return kingdomConcept;
	}

	/**
	 * @param kingdomConcept the kingdomConcept to set
	 */
	public void setKingdomConcept(TaxonConceptLite kingdomConcept) {
		this.kingdomConcept = kingdomConcept;
	}

	/**
	 * @return the kingdomConceptId
	 */
	public Long getKingdomConceptId() {
		return kingdomConceptId;
	}

	/**
	 * @param kingdomConceptId the kingdomConceptId to set
	 */
	public void setKingdomConceptId(Long kingdomConceptId) {
		this.kingdomConceptId = kingdomConceptId;
	}

	/**
	 * @return the phylumConcept
	 */
	public TaxonConceptLite getPhylumConcept() {
		return phylumConcept;
	}

	/**
	 * @param phylumConcept the phylumConcept to set
	 */
	public void setPhylumConcept(TaxonConceptLite phylumConcept) {
		this.phylumConcept = phylumConcept;
	}

	/**
	 * @return the phylumConceptId
	 */
	public Long getPhylumConceptId() {
		return phylumConceptId;
	}

	/**
	 * @param phylumConceptId the phylumConceptId to set
	 */
	public void setPhylumConceptId(Long phylumConceptId) {
		this.phylumConceptId = phylumConceptId;
	}

	/**
	 * @return the classConcept
	 */
	public TaxonConceptLite getClassConcept() {
		return classConcept;
	}

	/**
	 * @param classConcept the classConcept to set
	 */
	public void setClassConcept(TaxonConceptLite classConcept) {
		this.classConcept = classConcept;
	}

	/**
	 * @return the classConceptId
	 */
	public Long getClassConceptId() {
		return classConceptId;
	}

	/**
	 * @param classConceptId the classConceptId to set
	 */
	public void setClassConceptId(Long classConceptId) {
		this.classConceptId = classConceptId;
	}

	/**
	 * @return the orderConcept
	 */
	public TaxonConceptLite getOrderConcept() {
		return orderConcept;
	}

	/**
	 * @param orderConcept the orderConcept to set
	 */
	public void setOrderConcept(TaxonConceptLite orderConcept) {
		this.orderConcept = orderConcept;
	}

	/**
	 * @return the orderConceptId
	 */
	public Long getOrderConceptId() {
		return orderConceptId;
	}

	/**
	 * @param orderConceptId the orderConceptId to set
	 */
	public void setOrderConceptId(Long orderConceptId) {
		this.orderConceptId = orderConceptId;
	}

	/**
	 * @return the familyConcept
	 */
	public TaxonConceptLite getFamilyConcept() {
		return familyConcept;
	}

	/**
	 * @param familyConcept the familyConcept to set
	 */
	public void setFamilyConcept(TaxonConceptLite familyConcept) {
		this.familyConcept = familyConcept;
	}

	/**
	 * @return the familyConceptId
	 */
	public Long getFamilyConceptId() {
		return familyConceptId;
	}

	/**
	 * @param familyConceptId the familyConceptId to set
	 */
	public void setFamilyConceptId(Long familyConceptId) {
		this.familyConceptId = familyConceptId;
	}

	/**
	 * @return the genusConcept
	 */
	public TaxonConceptLite getGenusConcept() {
		return genusConcept;
	}

	/**
	 * @param genusConcept the genusConcept to set
	 */
	public void setGenusConcept(TaxonConceptLite genusConcept) {
		this.genusConcept = genusConcept;
	}

	/**
	 * @return the genusConceptId
	 */
	public Long getGenusConceptId() {
		return genusConceptId;
	}

	/**
	 * @param genusConceptId the genusConceptId to set
	 */
	public void setGenusConceptId(Long genusConceptId) {
		this.genusConceptId = genusConceptId;
	}

	/**
	 * @return the speciesConcept
	 */
	public TaxonConceptLite getSpeciesConcept() {
		return speciesConcept;
	}

	/**
	 * @param speciesConcept the speciesConcept to set
	 */
	public void setSpeciesConcept(TaxonConceptLite speciesConcept) {
		this.speciesConcept = speciesConcept;
	}

	/**
	 * @return the speciesConceptId
	 */
	public Long getSpeciesConceptId() {
		return speciesConceptId;
	}

	/**
	 * @param speciesConceptId the speciesConceptId to set
	 */
	public void setSpeciesConceptId(Long speciesConceptId) {
		this.speciesConceptId = speciesConceptId;
	}

	/**
	 * @return the mod360CellId
	 */
	public Integer getMod360CellId() {
		return mod360CellId;
	}

	/**
	 * @param mod360CellId the mod360CellId to set
	 */
	public void setMod360CellId(Integer mod360CellId) {
		this.mod360CellId = mod360CellId;
	}

	/**
	 * @return the deleted
	 */
	public Timestamp getDeleted() {
		return deleted;
	}

	/**
	 * @param deleted the deleted to set
	 */
	public void setDeleted(Timestamp deleted) {
		this.deleted = deleted;
	}

	/**
	 * @return the institutionCode
	 */
	public IdentifierCode getInstitutionCode() {
		return institutionCode;
	}

	/**
	 * @param institutionCode the institutionCode to set
	 */
	public void setInstitutionCode(IdentifierCode institutionCode) {
		this.institutionCode = institutionCode;
	}

	/**
	 * @return the collectionCode
	 */
	public IdentifierCode getCollectionCode() {
		return collectionCode;
	}

	/**
	 * @param collectionCode the collectionCode to set
	 */
	public void setCollectionCode(IdentifierCode collectionCode) {
		this.collectionCode = collectionCode;
	}

	/**
	 * @return the catalogueNumber
	 */
	public IdentifierCode getCatalogueNumber() {
		return catalogueNumber;
	}

	/**
	 * @param catalogueNumber the catalogueNumber to set
	 */
	public void setCatalogueNumber(IdentifierCode catalogueNumber) {
		this.catalogueNumber = catalogueNumber;
	}

	/**
	 * @return the modified
	 */
	public Timestamp getModified() {
		return modified;
	}

	/**
	 * @param modified the modified to set
	 */
	public void setModified(Timestamp modified) {
		this.modified = modified;
	}

	/**
   * @return the altitudeInMetres
   */
  public Integer getAltitudeInMetres() {
  	return altitudeInMetres;
  }

	/**
   * @param altitudeInMetres the altitudeInMetres to set
   */
  public void setAltitudeInMetres(Integer altitudeInMetres) {
  	this.altitudeInMetres = altitudeInMetres;
  }

  /**
   * @return the depthInCentimetres
   */
  public Float getDepthInCentimetres() {
  	return depthInCentimetres;
  }

  /**
   * @param depthInCentimetres the depthInCentimetres to set
   */
  public void setDepthInCentimetres(Float depthInCentimetres) {
	this.depthInCentimetres = depthInCentimetres;
  }

	/**
	 * @return the geoMappings
	 */
	public Set<GeoMapping> getGeoMappings() {
		return geoMappings;
	}
	
	/**
	 * @param geoMappings the geoMappings to set
	 */
	public void setGeoMappings(Set<GeoMapping> geoMappings) {
		this.geoMappings = geoMappings;
	}
	
	/**
	 * @return the tenmilliCellId
	 */
	public Integer getTenmilliCellId() {
		return tenmilliCellId;
	}
	
	/**
	 * @param tenmilliCellId the tenmilliCellId to set
	 */
	public void setTenmilliCellId(Integer tenmilliCellId) {
		this.tenmilliCellId = tenmilliCellId;
	}
}
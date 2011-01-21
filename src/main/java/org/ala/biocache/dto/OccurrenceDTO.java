/*
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 * 
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 * 
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 */

package org.ala.biocache.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ala.biocache.web.CustomDateSerializer;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.beans.Field;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Occurrence DTO bean to be populated by SOLR query via SOLRJ
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class OccurrenceDTO implements Serializable {
	
	private static final long serialVersionUID = -4501626905355608083L;
	/*
     * Fields corresponding to indexed fields in SOLR
     */
    @Field private String id;
	@Field("user_id") private String userId;
    // dataset
	@Field("data_provider_uid") private String dataProviderUid;
	@Field("data_provider") private String dataProvider;
	@Field("data_resource_uid") private String dataResourceUid;
	@Field("data_resource") private String dataResource;
	@Field("institution_code_uid") private String institutionCodeUid;
	@Field("institution_code") private String institutionCode;
	@Field("institution_code_name") private String institutionCodeName;
	@Field("institution_code_lsid") private String institutionCodeLsid;
	@Field("collection_code_uid") private String collectionCodeUid;
	@Field("collection_code") private String collectionCode;
	@Field("catalogue_number_id") private Integer catalogueNumberId;
	@Field("catalogue_number") private String catalogueNumber;
	@Field("citation") private String citation;
	@Field("taxon_concept_lsid") private String taxonConceptLsid;
	@Field private String year;
	@Field private String month;
    @Field("occurrence_date") private Date occurrenceDate;
	@Field("basis_of_record_id") private Integer basisOfRecordId;
	@Field("basis_of_record") private String basisOfRecord;
	@Field("raw_basis_of_record") private String rawBasisOfRecord;
	@Field private String collector;
	@Field("type_status") private String typeStatus;
	@Field("identifier_type") private String identifierType;
	@Field("identifier_value") private String identifierValue;
	@Field("identifier_name") private String identifierName;
	@Field("identifier_date") private Date identifierDate;
    @Field("individual_count") private Integer individualCount;
    @Field("location_remarks") private String locationRemarks;
    @Field("occurrence_remarks") private String occurrenceRemarks;
    // taxonomy
	@Field("taxon_name") private String taxonName;
    @Field("common_name") private String commonName;
	@Field private String author;
    @Field("rank_id") private Integer rankId;
    @Field private String rank;
	@Field("raw_taxon_name") private String rawTaxonName;
	@Field("raw_author") private String rawAuthor;
    @Field("raw_common_name") private String rawCommonName;
    @Field("lft") private Integer left;
    @Field("rgt") private Integer right;
	@Field("kingdom_lsid") private String kingdomLsid;
	@Field private String kingdom;
    @Field("phylum_lsid") private String phylumLsid;
	@Field private String phylum;
    @Field("class_lsid") private String classLsid;
	@Field("class") private String clazz;
    @Field("order_lsid") private String orderLsid;
	@Field private String order;
	@Field("family_lsid") private String familyLsid;
	@Field private String family;
    @Field("genus_lsid") private String genusLsid;
	@Field private String genus;
    @Field("species_lsid") private String speciesLsid;
	@Field private String species;    
    // geospatial
	@Field("country_code") private String countryCode;
	@Field("state") private List<String> states = new ArrayList<String>();
	@Field("biogeographic_region") private List<String> biogeographicRegions = new ArrayList<String>();
	@Field private List<String> places = new ArrayList<String>();
	@Field private Double latitude;
	@Field private Double longitude;
    @Field("lat_long_precision") private String coordinatePrecision;
    @Field("lat_long") private String latLong;
	@Field("cell_id") private Integer cellId;
	@Field("centi_cell_id") private Integer centiCellId;
	@Field("tenmilli_cell_id") private Integer tenmilliCellId;
    @Field("generalised_metres") private Integer generalisedMetres;
    @Field("geodetic_datum") private String geodeticDatum;
        
    // other
	@Field("taxonomic_issue") private String taxonomicIssue;
	@Field("geospatial_issue") private String geospatialIssue;
	@Field("other_issue") private String otherIssue;
    @Field("created_date") private Date createdDate;
	@Field("modified_date") private Date modifiedDate;
	@Field("point-1") private String point1;
	@Field("point-0.1") private String point01;
	@Field("point-0.01") private String point001;
	@Field("point-0.001") private String point0001;
	@Field("point-0.0001") private String point00001;
	@Field("names_lsid") private String namesLsid;
	@Field("confidence") private int confidence = 1;
	
    /*
     * Getters & Setters
     */
    
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getBasisOfRecord() {
        return basisOfRecord;
    }

    public void setBasisOfRecord(String basisOfRecord) {
        this.basisOfRecord = basisOfRecord;
    }

    public Integer getBasisOfRecordId() {
        return basisOfRecordId;
    }

    public void setBasisOfRecordId(Integer basisOfRecordId) {
        this.basisOfRecordId = basisOfRecordId;
    }

    public List<String> getBiogeographicRegions() {
        return biogeographicRegions;
    }

    public String getBiogeographicRegion() {
        return StringUtils.join(biogeographicRegions, "; ");
    }

    public void setBiogeographicRegions(List<String> biogeographicRegion) {
        this.biogeographicRegions = biogeographicRegion;
    }

    public String getCatalogueNumber() {
        return catalogueNumber;
    }

    public void setCatalogueNumber(String catalogueNumber) {
        this.catalogueNumber = catalogueNumber;
    }

    public Integer getCatalogueNumberId() {
        return catalogueNumberId;
    }

    public void setCatalogueNumberId(Integer catalogueNumberId) {
        this.catalogueNumberId = catalogueNumberId;
    }

    public Integer getCellId() {
        return cellId;
    }

    public void setCellId(Integer cellId) {
        this.cellId = cellId;
    }

    public Integer getCentiCellId() {
        return centiCellId;
    }

    public void setCentiCellId(Integer centiCellId) {
        this.centiCellId = centiCellId;
    }

    public String getClassLsid() {
        return classLsid;
    }

    public void setClassLsid(String classLsid) {
        this.classLsid = classLsid;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getCollectionCode() {
        return collectionCode;
    }

    public void setCollectionCode(String collectionCode) {
        this.collectionCode = collectionCode;
    }

    public String getCollectionCodeUid() {
        return collectionCodeUid;
    }

    public void setCollectionCodeUid(String collectionCodeUid) {
        this.collectionCodeUid = collectionCodeUid;
    }

    public String getCollector() {
        return collector;
    }

    public void setCollector(String collector) {
        this.collector = collector;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getDataProvider() {
        return dataProvider;
    }

    public void setDataProvider(String dataProvider) {
        this.dataProvider = dataProvider;
    }

    public String getDataProviderUid() {
        return dataProviderUid;
    }

    public void setDataProviderUid(String dataProviderUid) {
        this.dataProviderUid = dataProviderUid;
    }

    public String getDataResource() {
        return dataResource;
    }

    public void setDataResource(String dataResource) {
        this.dataResource = dataResource;
    }

    public String getDataResourceUid() {
        return dataResourceUid;
    }

    public void setDataResourceUid(String dataResourceUid) {
        this.dataResourceUid = dataResourceUid;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getFamilyLsid() {
        return familyLsid;
    }

    public void setFamilyLsid(String familyLsid) {
        this.familyLsid = familyLsid;
    }

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }

    public String getGenusLsid() {
        return genusLsid;
    }

    public void setGenusLsid(String genusLsid) {
        this.genusLsid = genusLsid;
    }

    public String getGeospatialIssue() {
        return geospatialIssue;
    }

    public void setGeospatialIssue(String geospatialIssue) {
        this.geospatialIssue = geospatialIssue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonSerialize(using=CustomDateSerializer.class)
    public Date getIdentifierDate() {
        return identifierDate;
    }

    public void setIdentifierDate(Date identifierDate) {
        this.identifierDate = identifierDate;
    }

    public String getIdentifierName() {
        return identifierName;
    }

    public void setIdentifierName(String identifierName) {
        this.identifierName = identifierName;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    public String getIdentifierValue() {
        return identifierValue;
    }

    public void setIdentifierValue(String identifierValue) {
        this.identifierValue = identifierValue;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    public String getInstitutionCodeUid() {
        return institutionCodeUid;
    }

    public void setInstitutionCodeUid(String institutionCodeUid) {
        this.institutionCodeUid = institutionCodeUid;
    }

    public String getInstitutionCodeLsid() {
        return institutionCodeLsid;
    }

    public void setInstitutionCodeLsid(String institutionCodeLsid) {
        this.institutionCodeLsid = institutionCodeLsid;
    }

    public String getInstitutionCodeName() {
        return institutionCodeName;
    }

    public void setInstitutionCodeName(String institutionCodeName) {
        this.institutionCodeName = institutionCodeName;
    }

    public String getKingdom() {
        return kingdom;
    }

    public void setKingdom(String kingdom) {
        this.kingdom = kingdom;
    }

    public String getKingdomLsid() {
        return kingdomLsid;
    }

    public void setKingdomLsid(String kingdomLsid) {
        this.kingdomLsid = kingdomLsid;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @JsonSerialize(using = CustomDateSerializer.class)
    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    @JsonSerialize(using = CustomDateSerializer.class)
    public Date getOccurrenceDate() {
        return occurrenceDate;
    }

    public void setOccurrenceDate(Date value) {
        this.occurrenceDate = value;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getOrderLsid() {
        return orderLsid;
    }

    public void setOrderLsid(String orderLsid) {
        this.orderLsid = orderLsid;
    }

    public String getOtherIssue() {
        return otherIssue;
    }

    public void setOtherIssue(String otherIssue) {
        this.otherIssue = otherIssue;
    }

    public String getPhylum() {
        return phylum;
    }

    public void setPhylum(String phylum) {
        this.phylum = phylum;
    }

    public String getPhylumLsid() {
        return phylumLsid;
    }

    public void setPhylumLsid(String phylumLsid) {
        this.phylumLsid = phylumLsid;
    }

    public List<String> getPlaces() {
        return places;
    }

    public String getPlace() {
        return StringUtils.join(places, "; ");
    }

    public void setPlaces(List<String> places) {
        this.places = places;
    }

    public String getRawAuthor() {
        return rawAuthor;
    }

    public void setRawAuthor(String rawAuthor) {
        this.rawAuthor = rawAuthor;
    }

    public String getRawBasisOfRecord() {
        return rawBasisOfRecord;
    }

    public void setRawBasisOfRecord(String rawBasisOfRecord) {
        this.rawBasisOfRecord = rawBasisOfRecord;
    }

    public String getRawTaxonName() {
        return rawTaxonName;
    }

    public void setRawTaxonName(String rawTaxonName) {
        this.rawTaxonName = rawTaxonName;
    }

    public String getRawCommonName() {
        return rawCommonName;
    }

    public void setRawCommonName(String rawCommonName) {
        this.rawCommonName = rawCommonName;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getSpeciesLsid() {
        return speciesLsid;
    }

    public void setSpeciesLsid(String speciesLsid) {
        this.speciesLsid = speciesLsid;
    }

    public List<String> getStates() {
        return states;
    }

    public String getState() {
        return StringUtils.join(states, "; ");
    }

    public void setStates(List<String> states) {
        this.states = states;
    }

    public String getTaxonConceptLsid() {
        return taxonConceptLsid;
    }

    public void setTaxonConceptLsid(String taxonConceptLsid) {
        this.taxonConceptLsid = taxonConceptLsid;
    }

    public String getTaxonName() {
        return taxonName;
    }

    public void setTaxonName(String taxonName) {
        this.taxonName = taxonName;
    }

    public String getTaxonomicIssue() {
        return taxonomicIssue;
    }

    public void setTaxonomicIssue(String taxonomicIssue) {
        this.taxonomicIssue = taxonomicIssue;
    }

    public Integer getTenmilliCellId() {
        return tenmilliCellId;
    }

    public void setTenmilliCellId(Integer tenmilliCellId) {
        this.tenmilliCellId = tenmilliCellId;
    }

    public String getTypeStatus() {
        return typeStatus;
    }

    public void setTypeStatus(String typeStatus) {
        this.typeStatus = typeStatus;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getCoordinatePrecision() {
        return coordinatePrecision;
    }

    public void setCoordinatePrecision(String coordinatePrecision) {
        this.coordinatePrecision = coordinatePrecision;
    }

    @JsonSerialize(using = CustomDateSerializer.class)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public Integer getRankId() {
        return rankId;
    }

    public void setRankId(Integer rankId) {
        this.rankId = rankId;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public Integer getLeft() {
        return left;
    }

    public void setLeft(Integer left) {
        this.left = left;
    }

    public Integer getRight() {
        return right;
    }

    public void setRight(Integer right) {
        this.right = right;
    }

	/**
	 * @return the citation
	 */
	public String getCitation() {
		return citation;
	}

	/**
	 * @param citation the citation to set
	 */
	public void setCitation(String citation) {
		this.citation = citation;
	}

	/**
	 * @return the userId
	 */
	@JsonIgnore
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
	 * @return the point1
	 */
	public String getPoint1() {
		return point1;
	}

	/**
	 * @param point1 the point1 to set
	 */
	public void setPoint1(String point1) {
		this.point1 = point1;
	}

	/**
	 * @return the point01
	 */
	public String getPoint01() {
		return point01;
	}

	/**
	 * @param point01 the point01 to set
	 */
	public void setPoint01(String point01) {
		this.point01 = point01;
	}

	/**
	 * @return the point001
	 */
	public String getPoint001() {
		return point001;
	}

	/**
	 * @param point001 the point001 to set
	 */
	public void setPoint001(String point001) {
		this.point001 = point001;
	}

	/**
	 * @return the point0001
	 */
	public String getPoint0001() {
		return point0001;
	}

	/**
	 * @param point0001 the point0001 to set
	 */
	public void setPoint0001(String point0001) {
		this.point0001 = point0001;
	}

	/**
	 * @return the point00001
	 */
	public String getPoint00001() {
		return point00001;
	}

	/**
	 * @param point00001 the point00001 to set
	 */
	public void setPoint00001(String point00001) {
		this.point00001 = point00001;
	}

	/**
	 * @return the namesLsid
	 */
	public String getNamesLsid() {
		return namesLsid;
	}

	/**
	 * @param namesLsid the namesLsid to set
	 */
	public void setNamesLsid(String namesLsid) {
		this.namesLsid = namesLsid;
	}


    public Integer getGeneralisedMetres() {
        return generalisedMetres;
    }

    public void setGeneralisedMetres(Integer generalisedMetres) {
        this.generalisedMetres = generalisedMetres;
    }

    public String getGeodeticDatum() {
        return geodeticDatum;
    }

    public void setGeodeticDatum(String geodeticDatum) {
        this.geodeticDatum = geodeticDatum;
    }

    public Integer getIndividualCount() {
        return individualCount;
    }

    public void setIndividualCount(Integer individualCount) {
        this.individualCount = individualCount;
    }

    public String getLocationRemarks() {
        return locationRemarks;
    }

    public void setLocationRemarks(String locationRemarks) {
        this.locationRemarks = locationRemarks;
    }

    public String getOccurrenceRemarks() {
        return occurrenceRemarks;
    }

    public void setOccurrenceRemarks(String occurrenceRemarks) {
        this.occurrenceRemarks = occurrenceRemarks;
    }

	public int getConfidence() {
		return confidence;
	}

	public void setConfidence(int confidence) {
		this.confidence = confidence;
	}

    public String getLatLong() {
        return latLong;
    }

    public void setLatLong(String latLong) {
        this.latLong = latLong;
    }
    
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "OccurrenceDTO [id=" + id + ", userId=" + userId
				+ ", dataProviderUid=" + dataProviderUid + ", dataProvider="
				+ dataProvider + ", dataResourceId=" + dataResourceUid
				+ ", dataResource=" + dataResource + ", institutionCodeUid="
				+ institutionCodeUid + ", institutionCode=" + institutionCode
				+ ", institutionCodeName=" + institutionCodeName
				+ ", institutionCodeLsid=" + institutionCodeLsid
				+ ", collectionCodeUid=" + collectionCodeUid
				+ ", collectionCode=" + collectionCode + ", catalogueNumberId="
				+ catalogueNumberId + ", catalogueNumber=" + catalogueNumber
				+ ", citation=" + citation + ", taxonConceptLsid="
				+ taxonConceptLsid + ", year=" + year + ", month=" + month
				+ ", occurrenceDate=" + occurrenceDate + ", basisOfRecordId="
				+ basisOfRecordId + ", basisOfRecord=" + basisOfRecord
				+ ", rawBasisOfRecord=" + rawBasisOfRecord + ", collector="
				+ collector + ", typeStatus=" + typeStatus
				+ ", identifierType=" + identifierType + ", identifierValue="
				+ identifierValue + ", identifierName=" + identifierName
				+ ", identifierDate=" + identifierDate + ", taxonName="
				+ taxonName + ", commonName=" + commonName + ", author="
				+ author + ", rankId=" + rankId + ", rank=" + rank
				+ ", rawTaxonName=" + rawTaxonName + ", rawAuthor=" + rawAuthor
				+ ", left=" + left + ", right=" + right + ", kingdomLsid="
				+ kingdomLsid + ", kingdom=" + kingdom + ", phylumLsid="
				+ phylumLsid + ", phylum=" + phylum + ", classLsid="
				+ classLsid + ", clazz=" + clazz + ", orderLsid=" + orderLsid
				+ ", order=" + order + ", familyLsid=" + familyLsid
				+ ", family=" + family + ", genusLsid=" + genusLsid
				+ ", genus=" + genus + ", speciesLsid=" + speciesLsid
				+ ", species=" + species + ", countryCode=" + countryCode
				+ ", states=" + states + ", biogeographicRegions="
				+ biogeographicRegions + ", places=" + places + ", latitude="
				+ latitude + ", longitude=" + longitude
				+ ", coordinatePrecision=" + coordinatePrecision + ", cellId="
				+ cellId + ", centiCellId=" + centiCellId + ", tenmilliCellId="
				+ tenmilliCellId + ", taxonomicIssue=" + taxonomicIssue
				+ ", geospatialIssue=" + geospatialIssue + ", otherIssue="
				+ otherIssue + ", createdDate=" + createdDate
				+ ", modifiedDate=" + modifiedDate + ", point1=" + point1
				+ ", point01=" + point01 + ", point001=" + point001
				+ ", point0001=" + point0001 + ", point00001=" + point00001
                + ", latLong=" + latLong
				+ ", namesLsid=" + namesLsid + "]";
	}
}

package org.ala.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

/**
 * TaxonName impl using Hibernate Search Annotations
 * 
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
@Entity
@Indexed(index="TaxonConcepts")
@javax.persistence.Table(name="taxon_concept", schema="portal")
public class TaxonConcept {

	/** The primary key */
	@Id
	@GeneratedValue
	@Field(index=Index.NO, store=Store.YES)
	protected Long id;
	
	@ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="taxon_name_id")
    @IndexedEmbedded(depth=2)
	protected TaxonName taxonName;
	
	@Column(name="data_resource_id")
	protected Long dataResourceId;
	
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
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
}
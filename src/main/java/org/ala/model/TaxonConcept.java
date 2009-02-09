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
	
	@ManyToOne(fetch=FetchType.EAGER, targetEntity=TaxonConcept.class,optional=true)
	@JoinColumn(name="kingdom_concept_id")
	@IndexedEmbedded(depth=2)
	protected TaxonConcept kingdomConcept;
	
	@ManyToOne(fetch=FetchType.EAGER, targetEntity=TaxonConcept.class,optional=true)
	@JoinColumn(name="family_concept_id")
	@IndexedEmbedded(depth=2)
	protected TaxonConcept familyConcept;
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
	/**
	 * @return the kingdomConcept
	 */
	public TaxonConcept getKingdomConcept() {
		return kingdomConcept;
	}
	/**
	 * @param kingdomConcept the kingdomConcept to set
	 */
	public void setKingdomConcept(TaxonConcept kingdomConcept) {
		this.kingdomConcept = kingdomConcept;
	}
	/**
	 * @return the familyConcept
	 */
	public TaxonConcept getFamilyConcept() {
		return familyConcept;
	}
	/**
	 * @param familyConcept the familyConcept to set
	 */
	public void setFamilyConcept(TaxonConcept familyConcept) {
		this.familyConcept = familyConcept;
	}
}
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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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
@Indexed(index="TaxonNames")
@javax.persistence.Table(name="taxon_name", schema="portal")
public class TaxonName {

	/** The primary key */
	@Id
	@GeneratedValue
	protected Long id;

	/** The canonical name **/
	@Field(index=Index.TOKENIZED, store=Store.YES)
	@IndexedEmbedded(prefix="canonical")
	protected String canonical;
	
	/** The author of this name */
	@Field(index=Index.TOKENIZED, store=Store.YES)
	@IndexedEmbedded(prefix="author")
	protected String author;

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
	 * @return the canonical
	 */
	public String getCanonical() {
		return canonical;
	}
	/**
	 * @param canonical the canonical to set
	 */
	public void setCanonical(String canonical) {
		this.canonical = canonical;
	}
	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}
	/**
	 * @param author the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}
}
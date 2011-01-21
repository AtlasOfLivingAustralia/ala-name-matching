/**************************************************************************
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
 ***************************************************************************/
package org.ala.biocache.web;

import java.util.List;

import javax.inject.Inject;

import org.ala.biocache.dao.SearchDAO;
import org.ala.biocache.dto.FieldResultDTO;
import org.ala.biocache.dto.SearchQuery;
import org.ala.biocache.dto.TaxaRankCountDTO;
import org.ala.biocache.util.SearchUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A simple controller for providing breakdowns on top of the biocache.
 * 
 * FIXME Is this in use ?
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
@Controller
public class BreakdownController {

	@Inject
	protected SearchDAO searchDAO;

	protected SearchUtils searchUtils = new SearchUtils();

	protected String SPECIES_JSON = "breakdown/decade/species";
	
	protected String COLLECTIONS_JSON = "breakdown/decade/collections";

	/**
	 * 
	 * @param guid
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/breakdown/species/decades/{query}.json", method = RequestMethod.GET)
	public String speciesDecadeBreakdown(@PathVariable("query") String query,
			Model model) throws Exception {
		SearchQuery searchQuery = new SearchQuery(query, "collection");
		searchUtils.updateTaxonConceptSearchString(searchQuery);
		List<FieldResultDTO> results = searchDAO.findRecordByDecadeFor(searchQuery.getQuery());
		model.addAttribute("decades", results);
		return SPECIES_JSON;
	}

	/**
	 * 
	 * @param guid
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/breakdown/collection/decades/{query}.json", method = RequestMethod.GET)
	public String collectionDecadeBreakdown(
			@PathVariable("query") String query, Model model) throws Exception {
		SearchQuery searchQuery = new SearchQuery(query, "collection");
		searchUtils.updateCollectionSearchString(searchQuery);
		List<FieldResultDTO> results = searchDAO
				.findRecordByDecadeFor(searchQuery.getQuery());
		model.addAttribute("decades", results);
		return COLLECTIONS_JSON;
	}

	/**
	 * 
	 * @param guid
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/breakdown/species/states/{query}.json", method = RequestMethod.GET)
	public String speciesStateBreakdown(@PathVariable("query") String query,
			Model model) throws Exception {
		SearchQuery searchQuery = new SearchQuery(query, "collection");
		searchUtils.updateTaxonConceptSearchString(searchQuery);
		List<FieldResultDTO> results = searchDAO.findRecordByStateFor(searchQuery.getQuery());
		model.addAttribute("states", results);
		return SPECIES_JSON;
	}

	/**
	 * 
	 * @param guid
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/breakdown/collection/states/{query}.json", method = RequestMethod.GET)
	public String collectionStateBreakdown(@PathVariable("query") String query,
			Model model) throws Exception {
		SearchQuery searchQuery = new SearchQuery(query, "collection");
		searchUtils.updateCollectionSearchString(searchQuery);
		List<FieldResultDTO> results = searchDAO.findRecordByStateFor(searchQuery.getQuery());
		model.addAttribute("states", results);
		return COLLECTIONS_JSON;
	}

	/**
	 * 
	 * @param query
	 *            The UID to perform the breakdown for
	 * @param max
	 *            The maximum number of taxa allowed in the result set
	 * @param model
	 * @throws Exception
	 */
	@RequestMapping(value = "/breakdown/uid/taxa/{max}/{query}.json", method = RequestMethod.GET)
	public void uidTaxonBreakdown(@PathVariable("query") String query,
			@PathVariable("max") Integer max, Model model) throws Exception {
		String newQuery = SearchUtils.getUidSearchField(query);
		if (newQuery != null) {
			newQuery += ":" + query;
			TaxaRankCountDTO results = searchDAO.findTaxonCountForUid(newQuery,max);
			model.addAttribute("breakdown", results);
		}
	}

	/**
	 * Provides a breakdown of taxa that are part of the supplied name.
	 * 
	 * @param query
	 *            The UID to perform the breakdown on
	 * @param scientificName
	 *            The name of the taxon on which to perform the breakdown
	 * @param rank
	 *            The rank of the scientificName (the breakdown will include a
	 *            rank under this)
	 * @param model
	 * @throws Exception
	 */
	@RequestMapping(value = "/breakdown/uid/namerank/{query}.json*", method = RequestMethod.GET)
	public void uidNameRankTaxonBreakdown(
			@PathVariable("query") String query,
			@RequestParam(value = "name", required = false) String scientificName,
			@RequestParam(value = "rank", required = true) String rank,
			Model model) throws Exception {

		String newQuery = SearchUtils.getUidSearchField(query) + ":" + query;
		boolean includeSuppliedRank = true;
		if (newQuery != null && scientificName != null) {
			// add the scientific name to the search
			// we want to return the berak down for the next rank after this one
			newQuery += " AND " + rank + ":" + scientificName;
			includeSuppliedRank = false;
		}

		TaxaRankCountDTO results = searchDAO.findTaxonCountForUid(newQuery,
				rank, includeSuppliedRank);
		model.addAttribute("breakdown", results);

	}

	/**
	 * @param searchDAO
	 *            the searchDAO to set
	 */
	public void setSearchDAO(SearchDAO searchDAO) {
		this.searchDAO = searchDAO;
	}
}

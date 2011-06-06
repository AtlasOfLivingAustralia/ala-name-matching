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
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * A simple controller for providing breakdowns on top of the biocache.
 * 
 *  
 * @author Dave Martin (David.Martin@csiro.au)
 * @author Natasha Carter (Natasha.Carter@csiro.au)
 */
@Controller
public class BreakdownController {

	@Inject
	protected SearchDAO searchDAO;

	protected SearchUtils searchUtils = new SearchUtils();

	


        /**
         * Returns a breakdown of collection,institution by a specific rank where the breakdown is limited to the
         * supplied max number. The rank that is returned depends on which rank contains closest to max
         * distinct values.
         *
         * @param uuid
         * @param max
         * @param model
         * @return
         * @throws Exception
         */
        @RequestMapping(value = {"/breakdown/collections/{uuid}*","/breakdown/institutions/{uuid}*",
                                "/breakdown/data-resources/{uuid}*", "/breakdown/data-providers/{uuid}*", "/breakdown/data-hubs/{uuid}*"}, method = RequestMethod.GET)
        public @ResponseBody TaxaRankCountDTO collectionLimitBreakdown(@PathVariable("uuid") String uuid,
                        @RequestParam(value = "max", required = true) Integer max,
                        Model model) throws Exception {

            return searchDAO.findTaxonCountForUid(searchUtils.getUIDSearchString(uuid.split(",")), max);
        }
        /**
         * TODO change to individual services...
         * Returns a breakdown of institution by a specific rank where the breakdown is limited to the
         * supplied max number. The rank that is returned depends on which rank contains closest to max
         * distinct values.
         *
         * @param uuid
         * @param max
         * @param model
         * @return
         * @throws Exception
         */
//        @RequestMapping(value = "/breakdown/institutions/{uuid}*", method = RequestMethod.GET)
//        public TaxaRankCountDTO institutionLimitBreakdown(@PathVariable("uuid") String uuid,
//                        @RequestParam(value = "max", required = true) Integer max,
//                        Model model) throws Exception {
//
//            return searchDAO.findTaxonCountForUid(searchUtils.getUIDSearchString(uuid.split(","), "institution_uid"), max);
//        }

        /**
         * Performs a breakdown without limiting the collection or institution
         * @param uuid
         * @param max
         * @param model
         * @return
         * @throws Exception
         */
        @RequestMapping(value = {"/breakdown/institutions*","/breakdown/collections*", "/breakdown/data-resources*","/breakdowns/data-providers*","/breakdowns/data-hubs*"}, method = RequestMethod.GET)
        public @ResponseBody TaxaRankCountDTO limitBreakdown(
                        @RequestParam(value = "max", required = true) Integer max,
                        Model model) throws Exception {
            return searchDAO.findTaxonCountForUid("*:*", max);
        }



        @RequestMapping(value = {"/breakdown/collections/{uuid}/rank/{rank}/name/{name}*", "/breakdown/institutions/{uuid}/rank/{rank}/name/{name}*",
                                 "/breakdown/data-resources/{uuid}/rank/{rank}/name/{name}*", "/breakdown/data-providers/{uuid}/rank/{rank}/name/{name}*",
                                 "/breakdown/data-hubs/{uuid}/rank/{rank}/name/{name}*"}, method = RequestMethod.GET)
        public @ResponseBody TaxaRankCountDTO collectionRankNameBreakdown(
                        @PathVariable("uuid") String uuid,
                        @PathVariable("rank") String rank,
                        @PathVariable("name") String name,
                        @RequestParam(value="level", required=false) String breakdownLevel,
                        Model model) throws Exception {

             String query = searchUtils.getUIDSearchString(uuid.split(","));
             query +=" AND " + rank + ":" + name;

           return searchDAO.findTaxonCountForUid(query,
				rank,breakdownLevel, false);
        }

        @RequestMapping(value = {"/breakdown/collections/rank/{rank}/name/{name}", "/breakdown/institutions/rank/{rank}/name/{name}",
                                 "/breakdown/data-providers/rank/{rank}/name/{name}", "/breakdown/data-resources/rank/{rank}/name/{name}",
                                 "/breakdown/data-hubs/rank/{rank}/name/{name}"}, method = RequestMethod.GET)
        public @ResponseBody TaxaRankCountDTO rankNameBreakdown(
                        @PathVariable("rank") String rank,
                        @PathVariable("name") String name,
                        Model model) throws Exception {

             String query =  rank + ":" + name;

           return searchDAO.findTaxonCountForUid(query,
				rank,null, false);
        }

        @RequestMapping(value = {"/breakdown/collections/{uuid}/rank/{rank}", "/breakdown/institutions/{uuid}/rank/{rank}",
                                 "/breakdown/data-resources/{uuid}/rank/{rank}", "/breakdown/data-providers/{uuid}/rank/{rank}",
                                 "/breakdown/data-hubs/{uuid}/rank/{rank}"}, method = RequestMethod.GET)
        public @ResponseBody TaxaRankCountDTO collectionRankBreakdown(
                        @PathVariable("uuid") String uuid,
                        @PathVariable("rank") String rank,
                        Model model) throws Exception {

             String query = searchUtils.getUIDSearchString(uuid.split(","));


           return searchDAO.findTaxonCountForUid(query,
				rank,null, true);
        }

        @RequestMapping(value = {"/breakdown/collections/rank/{rank}", "/breakdown/institutions/rank/{rank}",
                                 "/breakdown/data-providers/rank/{rank}", "/breakdown/data-resources/rank/{rank}",
                                 "/breakdown/data-hubs/rank/{rank}"}, method = RequestMethod.GET)
        public @ResponseBody TaxaRankCountDTO rankBreakdown(
                        @PathVariable("rank") String rank,
                        Model model) throws Exception {

             String query = "*:*";

           return searchDAO.findTaxonCountForUid(query,
				rank,null, false);
        }


	/**
	 * @param searchDAO
	 *            the searchDAO to set
	 */
	public void setSearchDAO(SearchDAO searchDAO) {
		this.searchDAO = searchDAO;
	}
}

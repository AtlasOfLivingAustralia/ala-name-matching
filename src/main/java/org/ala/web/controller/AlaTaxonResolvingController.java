/* *************************************************************************
 *  Copyright (C) 2009 Atlas of Living Australia
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

package org.ala.web.controller;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ala.dao.OccurrenceFacetDAO;
import org.gbif.portal.dto.taxonomy.BriefTaxonConceptDTO;
import org.gbif.portal.web.controller.taxonomy.TaxonResolvingController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * A Controller that extends TaxonResolvingController and adds a new model
 * for displaying facet breakdown charts.
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class AlaTaxonResolvingController extends TaxonResolvingController {
    /** The OccurrenceFacetDAO */
    protected OccurrenceFacetDAO occurrenceFacetDAO;

    @Override
	public ModelAndView handleRequest(Map<String, String> propertiesMap, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView mav = super.handleRequest(propertiesMap, request, response);

        String conceptIdentifier = propertiesMap.get(idRequestKey);
        BriefTaxonConceptDTO taxonConceptDTO = null;

        if (taxonomyManager.isValidTaxonConceptKey(conceptIdentifier)) {
            taxonConceptDTO = taxonomyManager.getTaxonConceptFor(conceptIdentifier,  RequestContextUtils.getLocale(request).getLanguage());
            Map<String, String> chartData = occurrenceFacetDAO.getChartFacetsForSpecies(conceptIdentifier, taxonConceptDTO);

            if (chartData != null) {
                mav.addObject("chartData", chartData);
            }
        }
        
        return mav;
    }

    /**
     * Setter for occurrenceFacetDAO
     * 
     * @param occurrenceFacetDAO the OccurrenceFacetDAO to set
     */
    public void setOccurrenceFacetDAO(OccurrenceFacetDAO occurrenceFacetDAO) {
        this.occurrenceFacetDAO = occurrenceFacetDAO;
    }
    
}

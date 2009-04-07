/*
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
 */

package org.ala.dao;

import java.util.List;
import java.util.Map;
import org.ala.model.GeoRegionTaxonConcept;
import org.ala.model.OccurrenceSearchCounts;
import org.gbif.portal.dto.taxonomy.BriefTaxonConceptDTO;

/**
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public interface OccurrenceFacetDAO {

    /**
     * 
     * @param dataResourceKey
     * @return Map
     */
    public Map<String, String> getChartFacetsForResource(String dataResourceKey);

    /**
     *
     * @param conceptIdentifier
     * @return Map
     */
    public Map<String, String> getChartFacetsForSpecies(String taxonConceptKey, BriefTaxonConceptDTO taxonConceptDTO);

    /**
     * 
     * @param regionId
     * @param startingRank
     * @return Map
     */
    public List<GeoRegionTaxonConcept> getTaxonConceptsForGeoRegion(Long regionId, String rankFacet);

    /**
     *
     * @param cellId
     * @param centiCellId
     * @param tenMilliCellId
     * @return
     */
    public OccurrenceSearchCounts getChartFacetsForMapCell(Integer cellId, Integer centiCellId, Integer tenMilliCellId, String entityPath, String entityId);

}

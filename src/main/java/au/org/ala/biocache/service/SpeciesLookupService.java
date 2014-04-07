/**************************************************************************
 *  Copyright (C) 2011 Atlas of Living Australia
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
package au.org.ala.biocache.service;

import java.util.List;
import java.util.Map;

/**
 * Service layer interface for accessing species lookups.
 * 
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public interface SpeciesLookupService {
	
    /**
     * Lookup a GUID for a given scientific name (returns best match/accepted concept)
     * 
     * @param name
     * @return 
     */
    String getGuidForName(String name);

    /**
     * Lookup the accepted name for a GUID
     *
     * @return
     */
    String getAcceptedNameForGuid(String guid);
    
    /**
     * Lookup service for list of guids to names
     * @param guids
     * @return
     */
    List<String> getNamesForGuids(List<String> guids);
    
    /**
     * Looks up the complete taxon concept details for the supplied guids in a bulk manner. 
     * @param guids
     * @return
     */
    List<Map<String,String>> getNameDetailsForGuids(List<String> guids);
    
    /**
     * Gets a all the names associated with the supplied list of guids.
     * @param guids
     * @return
     */
    Map<String,List<Map<String, String>>> getSynonymDetailsForGuids(List<String> guids);

    /**
     * Retrieves an list of arrays that contains the information that need to be included in the CSV details.
     * @param guids The guild/lsids for the species to get the details about
     * @param counts The corresponding counts
     * @param includeCounts whether or not to included the count in the details row
     * @param includeSynonyms whether or not whether or not synonyms should be included in the details row
     * @return a list of arrays to use as rows for the CSV species list download
     */
    List<String[]> getSpeciesDetails(List<String> guids,List<Long> counts, boolean includeCounts, boolean includeSynonyms);

    /**
     * Returns the header fields to use based on the species lookup service. Different implementations may include different fields.
     * @param field The field name that is causing the lookup to occur.
     * @param includeCounts whether or not counts should be included in the header
     * @param includeSynonyms whether or not synonyms should be included in the header
     * @return The header row to be used in a CSV species list
     */
    String[] getHeaderDetails(String field,boolean includeCounts, boolean includeSynonyms);
}

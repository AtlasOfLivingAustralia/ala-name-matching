package org.ala.biocache.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.org.ala.biocache.TaxonProfile;
import org.ala.biocache.dto.OccurrenceDTO;
import org.ala.biocache.dto.OccurrenceSource;
import org.ala.biocache.dto.OccurrenceSourceDTO;
import org.ala.biocache.dto.SearchQuery;
import org.ala.biocache.web.OccurrenceController;
import org.apache.commons.math.util.MathUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import atg.taglib.json.util.JSONObject;
import au.org.ala.biocache.TaxonProfileDAO;
import org.ala.biocache.dto.SearchRequestParams;
import org.ala.biocache.dto.SpatialSearchRequestParams;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import scala.Option;

/**
 * A class to provide utility methods used to populate search details.
 *
 * @author Natasha
 */
@Component("searchUtils")
public class SearchUtils {

	/** Logger initialisation */
	private final static Logger logger = Logger.getLogger(SearchUtils.class);
      
	protected String collectoryBaseUrl = "http://collections.ala.org.au";

	protected String bieBaseUrl = "http://bie.ala.org.au";

	private static final List<String> ranks = (List<String>) org.springframework.util.CollectionUtils
			.arrayToList(new String[] { "kingdom", "phylum", "class", "order",
					"family", "genus", "species" });

	/**
	 * Returns an array that contains the search string to use for a collection
	 * search and display name for the results.
	 *
	 * @param query
	 * @return true when UID could be located and query updated correctly
	 */
	public boolean updateCollectionSearchString(SearchQuery searchQuery) {
		try {
			String query = searchQuery.getQuery();

			// query the collectory for the institute and collection codes
			// needed to perform the search
			String jsonObject = OccurrenceController
					.getUrlContentAsString(collectoryBaseUrl
							+ "/lookup/summary/" + query);
			JSONObject j = new JSONObject(jsonObject);
			String collectionName = j.getString("name");
			// JSONArray institutionCode = j.getJSONArray("derivedInstCodes");
			// JSONArray collectionCode = j.getJSONArray("derivedCollCodes");
			StringBuilder displayString = new StringBuilder(getUidTitle(query)
					+ ": ");
			displayString.append(collectionName);
			// // Build Lucene query for institutions
			// StringBuilder solrQuery = new StringBuilder();
			// if (institutionCode != null && institutionCode.size() > 0) {
			//
			// List<String> institutions = new ArrayList<String>();
			// for (int i = 0; i < institutionCode.size(); i++) {
			// institutions.add("institution_code:" +
			// institutionCode.getString(i));
			// }
			// solrQuery.append("(");
			// solrQuery.append(StringUtils.join(institutions, " OR "));
			// solrQuery.append(")");
			//
			// }
			//
			// // Build Lucene query for collections
			// if (collectionCode != null && collectionCode.size() > 0) {
			// if (solrQuery.length() > 0) {
			// solrQuery.append(" AND ");
			//
			// }
			// //StringBuilder displayString = new
			// StringBuilder("Institution: ");
			// List<String> collections = new ArrayList<String>();
			// for (int i = 0; i < collectionCode.size(); i++) {
			// //quote the collection code to solve issue with invalid
			// characters (eg Invertebrates - Marine & Other)
			// collections.add("collection_code:\"" +
			// collectionCode.getString(i) +"\"");
			// // displayString.append(coll).append(" ");
			// }
			// solrQuery.append("(");
			// solrQuery.append(StringUtils.join(collections, " OR "));
			// solrQuery.append(")");
			//
			// }
			// searchQuery.setQuery("collection_code_uid:"+query);
			searchQuery.setQuery(getUidSearchField(query) + ":" + query);
			searchQuery.setDisplayString(displayString.toString());
			return true;

		} catch (Exception e) {
			logger.error(
					"Problem contacting the collectory: " + e.getMessage(), e);
			return false;
			// TODO work out what we want to do to the search if an exception
			// occurs while
			// contacting the collectory etc
		}

	}

        public boolean updateCollectionSearchString(SearchRequestParams searchParams, String uid) {
		try {


			// query the collectory for the institute and collection codes
			// needed to perform the search
                        String[] uids = uid.split(",");

			String jsonObject = OccurrenceController
					.getUrlContentAsString(collectoryBaseUrl
							+ "/lookup/summary/" + uids[0]);
			JSONObject j = new JSONObject(jsonObject);
			String collectionName = j.getString("name");
			// JSONArray institutionCode = j.getJSONArray("derivedInstCodes");
			// JSONArray collectionCode = j.getJSONArray("derivedCollCodes");
			StringBuilder displayString = new StringBuilder(getUidTitle(uids[0])
					+ ": ");
			displayString.append(collectionName);

			searchParams.setQ(getUIDSearchString(uids));
			searchParams.setDisplayString(displayString.toString());
                        updateCommon(searchParams);
			return true;

		} catch (Exception e) {
			logger.error("Problem contacting the collectory: " + e.getMessage(), e);
			return false;
			// TODO work out what we want to do to the search if an exception
			// occurs while
			// contacting the collectory etc
		}

	}

        public  String getUIDSearchString(String[] uids){
        StringBuilder sb = new StringBuilder();
        for(String uid : uids){
            if(sb.length()>0)
                sb.append(" OR ");
            sb.append(getUidSearchField(uid));
            sb.append(":");
            sb.append(uid);
        }
        return sb.toString();
    }


	/**
	 * Returns the filter query string required to perform a taxon concept
	 * search
	 *
	 * @param query
	 * @return
	 */
	public boolean updateTaxonConceptSearchString(SearchQuery searchQuery) {

		String guid = searchQuery.getQuery();

		Option<TaxonProfile> opt = TaxonProfileDAO.getByGuid(guid);

		//replace with webservice call or use biocache taxon profile cache - is this enough

		if (!opt.isEmpty()) {
                    TaxonProfile tc = opt.get();
                    StringBuffer entityQuerySb = new StringBuffer(tc.getRankString()
					+ ": " + tc.getScientificName());
			if (tc.getCommonName() != null) {
				entityQuerySb.append(" : ");
				entityQuerySb.append(tc.getCommonName());
			}
			searchQuery.addToFilterQuery("lft:[" + tc.getLeft() + " TO "
					+ tc.getRight() + "]");

			if (logger.isDebugEnabled()) {
				for (String filter : searchQuery.getFilterQuery()) {
					logger.debug("Filter: " + filter);
				}
			}
			searchQuery.setQuery("*:*");
			searchQuery.setEntityQuery(entityQuerySb.toString());
			return true;
		}
		return false;
	}

        /**
         * Updates the searchParams for a query by taxon concept
         * @param searchParams
         * @param guid
         * @return
         */
        public boolean updateTaxonConceptSearchString(SearchRequestParams searchParams, String guid) {
                // Get the taxon profile from the biocache cache - this could be replaced with a webservice call if necessary
		Option<TaxonProfile> opt = TaxonProfileDAO.getByGuid(guid);

		if (!opt.isEmpty()) {
                    TaxonProfile tc = opt.get();
                    StringBuffer entityQuerySb = new StringBuffer(tc.getRankString()
					+ ": " + tc.getScientificName());
			if (tc.getCommonName() != null) {
				entityQuerySb.append(" : ");
				entityQuerySb.append(tc.getCommonName());
			}
                        List<String> fq = new ArrayList<String>(java.util.Arrays.asList(searchParams.getFq()));

			fq.add("lft:[" + tc.getLeft() + " TO "
					+ tc.getRight() + "]");
                        searchParams.setFq(fq.toArray(new String[]{}));

			if (logger.isDebugEnabled()) {
				for (String filter : searchParams.getFq()) {
					logger.debug("Filter: " + filter);
				}
			}
			searchParams.setQ("*:*");
			searchParams.setDisplayString(entityQuerySb.toString());
                        updateCommon(searchParams);
			return true;
		}
		return false;
	}
        public static String getTaxonSearch(String lsid){
            // Get the taxon profile from the biocache cache - this could be replaced with a webservice call if necessary
            Option<TaxonProfile> opt = TaxonProfileDAO.getByGuid(lsid);
         
            if (!opt.isEmpty()) {
                TaxonProfile tc = opt.get();
                StringBuilder sb = new StringBuilder("lft:[");
                sb.append(tc.getLeft()).append(" TO ").append(tc.getRight()).append("]");
                return sb.toString();
            }
            //If the lsid for the taxon concept can not be found just return the original string
            return "lsid:" +lsid;
        }
        /**
         * updates the query ready for a spatial search
         * @param searchParams
         * @return
         */
        public boolean updateSpatial(SpatialSearchRequestParams searchParams){
            String query = searchParams.getQ();

            StringBuilder displayQuery = new StringBuilder(StringUtils.substringAfter(query, ":").replace("*", "(all taxa)"));
            displayQuery.append(" - within ").append(searchParams.getRadius()).append(" km of point (").append(searchParams.getLat()).append(", ").append(searchParams.getLon()).append(")");
            searchParams.setDisplayString(displayQuery.toString());
            query = formatSearchQuery(query);
             query  = "{!spatial lat=" + searchParams.getLat().toString() + " long=" + searchParams.getLon().toString() +
                " radius=" + searchParams.getRadius().toString() + " unit=km calc=arc threadCount=2}" + query; // calc=arc|plane
             searchParams.setQ(query);
            updateCommon(searchParams);
            return true;
        }
        public boolean updateNormal(SearchRequestParams searchParams){
            updateCommon(searchParams);
            return true;
        }
        /**
         * Formats the query string before
         * @param query
         * @return
         */
        protected String formatSearchQuery(String query) {
        // set the query
        StringBuilder queryString = new StringBuilder();
        if (query.equals("*:*") || query.contains(" AND ") || query.contains(" OR ") || query.startsWith("(")
                || query.endsWith("*") || query.startsWith("{")) {
            queryString.append(query);
        } else if (query.contains(":") && !query.startsWith("urn")) {
            // search with a field name specified (other than an LSID guid)
            String[] bits = StringUtils.split(query, ":", 2);
            queryString.append(ClientUtils.escapeQueryChars(bits[0]));
            queryString.append(":");
            queryString.append(ClientUtils.escapeQueryChars(bits[1]));
        } else {
            // regular search
            queryString.append(ClientUtils.escapeQueryChars(query));
        }
        return queryString.toString();
    }
        private void updateCommon(SearchRequestParams searchParams){
            // upate the filterQuery if it contains an "OccurrenceSource" so that it
		// has the correct filter query specified
//		String[] queries = searchParams.getFq();
//		if (queries != null) {
//			for (String q : queries) {
//				if (q.startsWith(OccurrenceSource.FACET_NAME + ":")) {
//					searchQuery.removeFromFilterQuery(q);
//					OccurrenceSource oc = OccurrenceSource.getForDisplayName(q
//							.substring(q.indexOf(":") + 1));
//					if (oc != null) {
//						searchQuery.addToFilterQuery("confidence:"
//								+ oc.getRange());
//					}
//				}
//			}
//		}
        }

	/**
	 * Returns the query to be used when searching for data providers.
	 *
	 * @param query
	 * @return
	 */

	public String getDataProviderSearchString(String query) {
		return "data_provider_id:" + query;
	}

	/**
	 * Returns the query to be used when searching for data resources.
	 *
	 * @param query
	 * @return
	 */
	public String getDataResourceSearchString(String query) {
		return "data_resource_id:" + query;
	}
//        public boolean updateQueryDetails(SearchRequestParams searchParams){
//
//
//            boolean success = true;
//		if (searchParams.getType().equals("collection")) {
//			success = updateCollectionSearchString(searchParams);
//		} else if (searchParams.getType().equals("provider")) {
//			searchParams.setQ(getDataProviderSearchString(searchParams
//					.getQ()));
//		} else if (searchParams.getType().equals("resource")) {
//			searchParams.setQ(getDataResourceSearchString(searchParams
//					.getQ()));
//		} else if (searchParams.getType().equals("taxon")) {
//			success = updateTaxonConceptSearchString(searchParams);
//		} else if (searchParams.getType().equals("spatial")){
//                    success = updateSpatial(searchParams);
//                }
//
//            return success;
//        }

	/**
	 * Returns the query string based on the type of search that needs to be
	 * performed.
	 *
	 * @param query
	 * @param type
	 * @return true when the query updated correctly, false otherwise
	 */
	public boolean updateQueryDetails(SearchQuery searchQuery) {
		logger.debug("Processing " + searchQuery.getQuery() + " using type: "
				+ searchQuery.getType());
		boolean success = true;
		if (searchQuery.getType().equals("collection")) {
			success = updateCollectionSearchString(searchQuery);
		} else if (searchQuery.getType().equals("provider")) {
			searchQuery.setQuery(getDataProviderSearchString(searchQuery
					.getQuery()));
		} else if (searchQuery.getType().equals("resource")) {
			searchQuery.setQuery(getDataResourceSearchString(searchQuery
					.getQuery()));
		} else if (searchQuery.getType().equals("taxon")) {
			success = updateTaxonConceptSearchString(searchQuery);
		}
		// otherwise we can leave the query with its default values ("normal"
		// type)

		// upate the filterQuery if it contains an "OccurrenceSource" so that it
		// has the correct filter query specified
		String[] queries = searchQuery.getFilterQuery();
		if (queries != null) {
			for (String q : queries) {
				if (q.startsWith(OccurrenceSource.FACET_NAME + ":")) {
					searchQuery.removeFromFilterQuery(q);
					OccurrenceSource oc = OccurrenceSource.getForDisplayName(q
							.substring(q.indexOf(":") + 1));
					if (oc != null) {
						searchQuery.addToFilterQuery("confidence:"
								+ oc.getRange());
					}
				}
			}
		}
		return success;

	}

	/**
	 * Set the initial point values in the index.
	 */
	public static void initialPointValues(OccurrenceDTO occurrence) {
		Double lat = occurrence.getLatitude();
		Double lon = occurrence.getLongitude();
		if (lat != null && lon != null) {
			occurrence.setPoint1(MathUtils.round(lat, 0) + ","
					+ MathUtils.round(lon, 0));
			occurrence.setPoint01(MathUtils.round(lat, 1) + ","
					+ MathUtils.round(lon, 1));
			occurrence.setPoint001(MathUtils.round(lat, 2) + ","
					+ MathUtils.round(lon, 2));
			occurrence.setPoint0001(MathUtils.round(lat, 3) + ","
					+ MathUtils.round(lon, 3));
			occurrence.setPoint00001(MathUtils.round(lat, 4) + ","
					+ MathUtils.round(lon, 4));
		}
	}

	/**
	 * returns the solr field that should be used to search for a particular uid
	 *
	 * @param uid
	 * @return
	 */
	public static String getUidSearchField(String uid) {
		if (uid.startsWith("co"))
			return "collection_code_uid";
		if (uid.startsWith("in"))
			return "institution_code_uid";
		if (uid.startsWith("dr"))
			return "data_resource_uid";
		if (uid.startsWith("dp"))
			return "data_provider_uid";
		return null;
	}

	/**
	 * returns the title that should be used to search for a particular uid
	 *
	 * @param uid
	 * @return
	 */
	public static String getUidTitle(String uid) {
		if (uid.startsWith("co"))
			return "Collection";
		if (uid.startsWith("in"))
			return "Institution";
		if (uid.startsWith("dr"))
			return "Data Resource";
		if (uid.startsWith("dp"))
			return "Data Provider";
		return null;
	}

	/**
	 * Returns the rank name based on an integer position
	 *
	 * @param position
	 * @return
	 */
	public static String getRankFacetName(int position) {
		switch (position) {
		case 1:
			return "kingdom";
		case 2:
			return "phylum";
		case 3:
			return "class";
		case 4:
			return "order";
		case 5:
			return "family";
		case 6:
			return "genus";
		case 7:
			return "species";
		default:
			return "unknown";
		}
	}

	/**
	 * Returns an ordered list of the next ranks after the supplied rank.
	 *
	 * @param rank
	 * @return
	 */
	public static List<String> getNextRanks(String rank,
			boolean includeSuppliedRank) {
		int start = includeSuppliedRank ? ranks.indexOf(rank) : ranks
				.indexOf(rank) + 1;
		if (start > 0)
			return ranks.subList(start, ranks.size());
		return ranks;
	}

	/**
	 * Returns the information for the supplied source keys
	 *
	 * TODO: There may be a better location for this method.
	 *
	 * @param keys
	 * @return
	 */
	public List<OccurrenceSourceDTO> getSourceInformation(
			Map<String, Integer> sources) {
		Set<String> keys = sources.keySet();
		logger.debug("Listing the source information for : " + keys);
		List<OccurrenceSourceDTO> lsources = new ArrayList<OccurrenceSourceDTO>();
		try {
			for (String key : keys) {
				// get the information for the uid
				String jsonObject = OccurrenceController
						.getUrlContentAsString(collectoryBaseUrl
								+ "/lookup/summary/" + key);
				JSONObject j = new JSONObject(jsonObject);
				lsources.add(new OccurrenceSourceDTO(j.getString("name"), key,
						sources.get(key)));
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		// sort the sources based on count
		java.util.Collections.sort(lsources,
				new java.util.Comparator<OccurrenceSourceDTO>() {

					@Override
					public int compare(OccurrenceSourceDTO o1,
							OccurrenceSourceDTO o2) {
						return o2.getCount() - o1.getCount();// sort the counts
																// in reverse
																// order so that
																// max count
																// appears at
																// the top of
																// the list
					}
				});
		return lsources;
	}

	/**
     * Provide default values for parameters if they have any null or "empty" values
     *
     * @param requestParams
     */
    public static void setDefaultParams(SearchRequestParams requestParams) {
        SearchRequestParams blankRequestParams = new SearchRequestParams(); // use for default values
        logger.debug("requestParams = " + requestParams);

        if (requestParams.getStart() == null) {
            requestParams.setStart(blankRequestParams.getStart());
        }

        if (requestParams.getPageSize() == null) {
            requestParams.setPageSize(blankRequestParams.getPageSize());
        }

        if (requestParams.getSort() == null || requestParams.getSort().isEmpty()) {
            requestParams.setSort(blankRequestParams.getSort());
        }

        if (requestParams.getDir() == null || requestParams.getDir().isEmpty()) {
            requestParams.setDir(blankRequestParams.getDir());
        }
    }

    /**
	 * @param collectoryBaseUrl
	 *            the collectoryBaseUrl to set
	 */
	public void setCollectoryBaseUrl(String collectoryBaseUrl) {
		this.collectoryBaseUrl = collectoryBaseUrl;
	}

	/**
	 * @param bieBaseUrl
	 *            the bieBaseUrl to set
	 */
	public void setBieBaseUrl(String bieBaseUrl) {
		this.bieBaseUrl = bieBaseUrl;
	}
}

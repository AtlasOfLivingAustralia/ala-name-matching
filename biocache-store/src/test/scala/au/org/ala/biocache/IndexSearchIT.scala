
package au.org.ala.biocache

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.FacetField
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocumentList
import org.junit.Ignore

@Ignore
object IndexSearchIT {
  implicit def iteratorToWrapper[T](iter:java.util.Iterator[T]):IteratorWrapper[T] = new IteratorWrapper[T](iter)
   def main(args: Array[String]): Unit = {
     System.out.println("Starting to search for Victoria")

    val solrQuery = new SolrQuery();
        solrQuery.setQueryType("standard");
        // Facets
        solrQuery.setFacet(true);
        solrQuery.addFacetField("basis_of_record");
        solrQuery.addFacetField("type_status");
        //solrQuery.addFacetField("data_resource");
        solrQuery.addFacetField("state");
        solrQuery.addFacetField("biogeographic_region");
        solrQuery.addFacetField("rank");
        solrQuery.addFacetField("kingdom");
        solrQuery.addFacetField("family");
        //solrQuery.addFacetField("data_provider");
        solrQuery.addFacetField("month");
        solrQuery.add("f.month.facet.sort","index"); // sort by Jan-Dec
        // Date Facet Params
        // facet.date=occurrence_date&facet.date.start=1900-01-01T12:00:00Z&facet.date.end=2010-01-01T12:00:00Z&facet.date.gap=%2B1YEAR
        solrQuery.add("facet.date","occurrence_date");
        solrQuery.add("facet.date.start", "1850-01-01T12:00:00Z"); // facet date range starts from 1850
        solrQuery.add("facet.date.end", "NOW/DAY"); // facet date range ends for current date (gap period)
        solrQuery.add("facet.date.gap", "+10YEAR"); // gap interval of 10 years
        solrQuery.add("facet.date.other", "before"); // include counts before the facet start date ("before" label)
        solrQuery.add("facet.date.include", "lower"); // counts will be included for dates on the starting date but not ending date

        //solrQuery.add("facet.date.other", "after");

        solrQuery.setFacetMinCount(1);
        solrQuery.setFacetLimit(30);
        solrQuery.setRows(10);
        solrQuery.setStart(0);
        solrQuery.setQuery("Victoria");

    val response = Config.getInstance(classOf[IndexDAO]).asInstanceOf[SolrIndexDAO].solrServer.query(solrQuery)
    processSolrResponse(response)
    System.out.println("response: " + response.getResults.size)
    
    System.exit(0)
   }


  def processSolrResponse(qr:QueryResponse) {

        val sdl:SolrDocumentList = qr.getResults();
       
        val facets:java.util.List[FacetField] = qr.getFacetFields();
        val facetDates:java.util.List[FacetField] = qr.getFacetDates();
        val facetQueries:java.util.Map[String, Integer] = qr.getFacetQuery();
        if (facetDates != null) {
            println("Facet dates size: "+facetDates.size());
            facets.addAll(facetDates);
        }
        else
          println("Facet dates are null")
        //Map<String, Map<String, List<String>>> highlights = qr.getHighlighting();
        //List<OccurrenceDTO> results = qr.getBeans(OccurrenceDTO.class);
        //List<FacetResultDTO> facetResults = new ArrayList<FacetResultDTO>();
        //searchResult.setTotalRecords(sdl.getNumFound());
        println("Total records found: "+ sdl.getNumFound)
        //searchResult.setStartIndex(sdl.getStart());
        println("Start: "+ sdl.getStart)
       
        //searchResult.setStatus("OK");
        //String[] solrSort = StringUtils.split(solrQuery.getSortField(), " "); // e.g. "taxon_name asc"
        //logger.debug("sortField post-split: "+StringUtils.join(solrSort, "|"));
        //searchResult.setSort(solrSort[0]); // sortField
        //searchResult.setDir(solrSort[1]); // sortDirection
        //searchResult.setQuery(solrQuery.getQuery());
        //searchResult.setOccurrences(results);
        // populate SOLR facet results
        if (facets != null) {
          for (val facet <- facets.iterator) {
                val facetEntries:java.util.List[FacetField.Count] = facet.getValues();
                if ((facetEntries != null) && (facetEntries.size() > 0)) {
                    
          for (val fcount <- facetEntries.iterator) {
//                        String msg = fcount.getName() + ": " + fcount.getCount();
                        println(fcount.getName() + ": " + fcount.getCount());
                        
                    }
                    
                }
            }
        }

   
        
    }

  
  class IteratorWrapper[A](iter:java.util.Iterator[A])
{
    def foreach(f: A => Unit): Unit = {
        while(iter.hasNext){
          f(iter.next)
        }
    }
}

}

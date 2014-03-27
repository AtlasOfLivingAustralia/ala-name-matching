package au.org.ala.biocache.export

import au.org.ala.biocache.util.OptionParser
import java.text.SimpleDateFormat
import org.apache.commons.lang3.time.DateUtils
import java.util.Date
import java.io.{File, FileWriter}
import au.org.ala.biocache.Config

object ExportFacet {

  var facetField = "species_guid"
  var facetQuery = "*:*"
  var facetOutputFile = "/tmp/facet-output-" + facetField + ".txt"
  var lastDay = false
  var lastWeek = false
  var lastMonth = false
  var includeCounts = false
  var indexDateField = "first_loaded_date"
  var closeIndex = true

  var fieldsToExport = Array[String]()

  val parser = new OptionParser("ExportFacet - Exports a facet to file") {
    arg("<facet-field>", "The field to facet on", {
      v: String => facetField = v
    })
    arg("<facet-output-file>", "The field to facet on", {
      v: String => facetOutputFile = v
    })
    opt("fq", "filter query", "Filter query to use", {
      v: String => facetQuery = v
    })
    opt("open", "Keep the index open", {
      closeIndex = false
    })
    booleanOpt("ld", "lastDay", "Only export those that have had new records in the last day", {
      v: Boolean => lastDay = v
    })
    booleanOpt("ld", "lastWeek", "Only export those that have had new records in the last week", {
      v: Boolean => lastWeek = v
    })
    booleanOpt("ld", "lastMonth", "Only export those that have had new records in the last month", {
      v: Boolean => lastMonth = v
    })
    booleanOpt("c", "incCounts", "Include the counts of the facet", {
      v: Boolean => includeCounts = v
    })
    opt("df", "date field to use", "The indexed date field to use e.g. first_loaded_Date", {
      v: String => indexDateField = v
    })
  }

  def main(args: Array[String]) {
    if (parser.parse(args)) {
      // first_loaded_date:[2012-03-26T00:00:00Z%20TO%20*]
      val sfd = new SimpleDateFormat("yyyy-MM-dd")
      var facetFilterQuery = ""
      if (lastDay) facetFilterQuery = indexDateField + ":[" + sfd.format(DateUtils.addDays(new Date(), -1)) + "T00:00:00Z TO *]"
      else if (lastWeek) facetFilterQuery = indexDateField + ":[" + sfd.format(DateUtils.addWeeks(new Date(), -1)) + "T00:00:00Z TO *]"
      else if (lastMonth) facetFilterQuery = indexDateField + ":[" + sfd.format(DateUtils.addMonths(new Date(), -1)) + "T00:00:00Z TO *]"

      //do the facet query
      val facetWriter = new FileWriter(new File(facetOutputFile))
      Config.indexDAO.pageOverFacet((label, count) => {
        facetWriter.write(label)
        if (includeCounts)
          facetWriter.write("\t" + count)
        facetWriter.write("\n")
        facetWriter.flush
        true
      }, facetField, facetQuery, Array(facetFilterQuery))
      facetWriter.flush
      facetWriter.close
      if (closeIndex)
        Config.indexDAO.shutdown
    }
  }
}
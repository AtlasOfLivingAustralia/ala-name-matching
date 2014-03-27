package au.org.ala.biocache.export

import au.org.ala.biocache.util.{StringConsumer, CountAwareFacetConsumer, OptionParser, FileHelper}
import java.io.{FileWriter, File}
import org.apache.commons.io.FileUtils
import java.util.Date
import java.util.concurrent.ArrayBlockingQueue
import au.org.ala.biocache.tool.DuplicationDetection

/**
 * Utility to export based on facet and optional filter.
 */
object ExportAllRecordFacetFilter {

  import FileHelper._

  def main(args: Array[String]) {

    var exportDirectory = "/data/offline/exports"
    var facet = ""
    var threads = 4
    var filter: Option[String] = None
    val fieldsToExport = Array("row_key", "id", "species_guid", "subspecies_guid", "year", "month", "occurrence_date", "point-1", "point-0.1",
      "point-0.01", "point-0.001", "point-0.0001", "lat_long", "raw_taxon_name", "collectors", "duplicate_status", "duplicate_record", "latitude", "longitude",
      "el882", "el889", "el887", "el865", "el894")
    val parser = new OptionParser("Export based on facet and optional filter") {
      arg("<output directory>", "the output directory for the exports", {
        v: String => exportDirectory = v
      })
      arg("<facet>", "The facet to base the download", {
        v: String => facet = v
      })
      intOpt("t", "threads", "the number of threads/files to have for the exports", {
        v: Int => threads = v
      })
      opt("f", "filter", "optional filter to apply to the list", {
        v: String => filter = Some(v)
      })
    }
    //last_load_date:["+lastRunDate.get+" TO *]
    if (parser.parse(args)) {
      val filename = exportDirectory + File.separator + "species-guids.txt"
      FileUtils.forceMkdir(new File(exportDirectory))
      val args2 = if (filter.isDefined) Array(facet, filename, "-fq", filter.get, "--open", "-c", "true") else Array(facet, filename, "--open", "-c", "true")
      println(new Date() + " Exporting the facets to be ued in the download")
      ExportFacet.main(args2)
      //now based on the number of threads download the other data
      val queue = new ArrayBlockingQueue[String](100)
      var ids = 0
      val pool: Array[Thread] = Array.fill(threads) {
        val file = new File(exportDirectory + File.separator + ids + File.separator + "species.out")
        val subspeciesfile = new File(exportDirectory + File.separator + ids + File.separator + "subspecies.out")
        FileUtils.forceMkdir(file.getParentFile)
        val fileWriter = new FileWriter(file)
        val p = new CountAwareFacetConsumer(queue, ids, {
          lsids =>
            val query = lsids.mkString("species_guid:\"", "\" OR species_guid:\"", "\"")
            DuplicationDetection.logger.info("Starting to download the occurrences for " + lsids.mkString(","))
            ExportByFacetQuery.downloadSingleTaxonByStream(query, null, fieldsToExport, "species_guid", Array("lat_long:[* TO *]"), Array("species_guid", "subspecies_guid", "row_key"), fileWriter, Some(new FileWriter(subspeciesfile)), Some(Array("duplicate_record")))
            fileWriter.flush()
          //at least 2 occurrences are necessary for the dump
        }, 10000, 2)

        ids += 1
        p.start
        p
      }

      //add to the queue
      new File(filename).foreachLine(line => queue.put(line.trim))

      pool.foreach(t => if (t.isInstanceOf[StringConsumer]) t.asInstanceOf[StringConsumer].shouldStop = true)
      pool.foreach(_.join)
    }
  }
}
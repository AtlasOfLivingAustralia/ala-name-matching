package au.org.ala.biocache.export

import au.org.ala.biocache.util.{OptionParser, FileHelper}
import java.io.{FileWriter, File}
import scala.collection.mutable.ArrayBuffer
import org.apache.commons.io.FileUtils
import au.org.ala.biocache.Config

/**
 * Uses one Streamer to write the spatial species to multiple files based on a number of "threads" that will be used to consume
 * the files.
 */
object ExportAllSpatialSpecies {

  import FileHelper._

  def main(args: Array[String]) {
    var threads = 4
    //Warning changing these fields may cause issues in the offline processing tasks
    val fieldsToExport = Array("row_key", "id", "species_guid", "subspecies_guid", "year", "month", "occurrence_date", "point-1", "point-0.1",
      "point-0.01", "point-0.001", "point-0.0001", "lat_long", "raw_taxon_name", "collectors", "duplicate_status", "duplicate_record", "latitude", "longitude",
      "el882", "el889", "el887", "el865", "el894", "coordinate_uncertainty")
    val query = "lat_long:* AND species_guid:*"
    //val query = "lat_long:* AND (species_guid:\"urn:lsid:biodiversity.org.au:afd.taxon:3428ab9c-1bf4-4542-947a-8ea048327c4c\" OR species_guid:\"urn:lsid:biodiversity.org.au:afd.taxon:33bd7bb6-f374-4d9c-80f8-248671c919cd\" OR species_guid:\"urn:lsid:biodiversity.org.au:afd.taxon:1a39ed75-0e3d-4fbd-bdda-ba51231911e0\" OR species_guid:\"urn:lsid:biodiversity.org.au:afd.taxon:98b232ae-b2fe-4c91-8b58-933aa608ab5e\")"
    val filterQueries = Array[String]()
    val sortFields = Array("species_guid", "subspecies_guid", "row_key")
    val multivaluedFields = Some(Array("duplicate_record"))
    var exportDirectory = "/data/offline/exports"
    var lastWeek = false
    var validGuids: Option[List[String]] = None

    val parser = new OptionParser("Export based on facet and optional filter") {
      arg("<output directory>", "the output directory for the exports", {
        v: String => exportDirectory = v
      })

      intOpt("t", "threads", "the number of threads/files to have for the exports", {
        v: Int => threads = v
      })
      opt("lastWeek", "species that have changed in the last week", {
        lastWeek = true
      })
      //opt("f","filter", "optional filter to apply to the list", {v:String => filter = Some(v)})
    }
    if (parser.parse(args)) {

      if (lastWeek) {
        //need to obtain a list of species guids that have changed in the last week
        def filename = exportDirectory + File.separator + "delta-species-guids.txt"
        val args = Array("species_guid", filename, "--lastWeek", "true", "--open")
        ExportFacet.main(args)
        //now load the acceptable lsids into the list
        val buf = new ArrayBuffer[String]()
        new File(filename).foreachLine(line => {
          buf += line
        })
        validGuids = Some(buf.toList)
        println("There are " + buf.size + " valid guids to download")
      }


      var ids = 0
      //construct all the file writers that will be randomly assigned taxon concepts
      val files: Array[(FileWriter, FileWriter)] = Array.fill(threads) {
        val file = new File(exportDirectory + File.separator + ids)
        FileUtils.forceMkdir(file)
        ids += 1
        (new FileWriter(new File(file.getAbsolutePath + File.separator + "species.out")), new FileWriter(new File(file.getAbsolutePath + File.separator + "subspecies.out")))
      }
      //val file = new File(exportDirectory +  File.separator + ids + File.separator+"species.out")
      //FileUtils.forceMkdir(file.getParentFile)
      //val subspeciesfile = new File(exportDirectory +  File.separator + ids + File.separator+"subspecies.out")
      //val fileWriter = new FileWriter(file)
      //val subspeciesWriter = new FileWriter(subspeciesfile)
      var counter = 0
      var currentLsid = ""
      var lsidCount = 0
      var fileWriter: FileWriter = null
      var subspeciesWriter: FileWriter = null
      var loadCurrent = true
      Config.indexDAO.streamIndex(map => {
        val outputLine = fieldsToExport.map(f => getFromMap(map, f)).mkString("\t")
        counter += 1
        val thisLsid = map.get("species_guid")
        if (thisLsid != null && thisLsid != currentLsid) {
          println("Starting to handle " + thisLsid + " " + counter + " " + lsidCount)

          currentLsid = thisLsid.toString
          loadCurrent = validGuids.isEmpty || validGuids.get.contains(currentLsid)
          if (loadCurrent) {
            lsidCount += 1
          }
          if (fileWriter != null) {
            fileWriter.flush
            subspeciesWriter.flush
          }
          fileWriter = files(lsidCount % threads)._1
          subspeciesWriter = files(lsidCount % threads)._2
        }

        if (loadCurrent) {
          fileWriter.write(outputLine)
          fileWriter.write("\n")

          val subspecies = map.get("subspecies_guid")
          if (subspecies != null) {
            subspeciesWriter.write(outputLine)
            subspeciesWriter.write("\n")
          }
        }

        if (counter % 10000 == 0) {
          fileWriter.flush
          subspeciesWriter.flush
        }

        true
      }, fieldsToExport, query, filterQueries, sortFields, multivaluedFields)

      files.foreach {
        case (fw1, fw2) => {
          fw1.flush()
          fw1.close()
          fw2.flush()
          fw2.close()
        }
      }
    }
  }

  def getFromMap(map: java.util.Map[String, AnyRef], key: String): String = {
    val value = map.get(key)
    if (value == null) "" else value.toString.replaceAll("(\r\n|\n)", " ")
  }
}

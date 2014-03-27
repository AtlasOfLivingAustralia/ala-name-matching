package au.org.ala.biocache.tool

import java.io.{FileWriter, File}
import org.apache.commons.io.FileUtils
import au.org.ala.biocache.export.ExportByFacetQuery

/**
 * Processes the record that have been reloaded since the last duplication detection.
 *
 * This should be a much quicker process than the full duplication detection
 */
class IncrementalDuplicationDetection(lastDupDate: String) extends DuplicationDetection {
  /**
   * Incremental duplication detection has different logic around downloading the records for consideration
   */
  override def downloadRecords(sourceFileName: String, lsid: String, field: String) {
    val file = new File(sourceFileName)
    FileUtils.forceMkdir(file.getParentFile)
    val fileWriter = new FileWriter(file)
    DuplicationDetection.logger.info("Starting to download the occurrences for " + lsid)
    def filters = (if (field == "species_guid") speciesFilters else subspeciesFilters) ++ Array("last_load_date:[" + lastDupDate + " TO *]")
    ExportByFacetQuery.downloadSingleTaxon(lsid, fieldsToExport, field, filters, Some("row_key"), Some("asc"), fileWriter)
    fileWriter.close
  }
}
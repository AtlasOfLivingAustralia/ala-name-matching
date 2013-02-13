package au.org.ala.util

import collection.mutable.ListBuffer
import java.text.MessageFormat
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import au.org.ala.biocache.Config
import org.apache.commons.codec.net.URLCodec
import org.apache.commons.math3.util.Precision
import org.apache.commons.io.IOUtils
import java.io.{BufferedWriter, FileWriter}
import org.apache.commons.lang3.StringUtils
import org.apache.solr.client.solrj.util.ClientUtils

/**
 * Created with IntelliJ IDEA.
 * User: ChrisF
 * Date: 29/11/12
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
object EndemismLayerHelper {
  // NOTE all queries filter out any occurrences that do not call within the bounding box 40E-172E and 8S-72S - this is a rough approximation of the
  // Australian EEZ.

  // All occurrences
  val ALL_SPECIES_QUERY = "species_guid:[* TO *] AND geospatial_kosher:true"
  val SPECIES_QUERY_TEMPLATE_ALL_OCCURRENCES = "species_guid:{0} AND geospatial_kosher:true"

  // Terrestrial (non-marine) occurrences
  val NON_MARINE_QUERY = "species_guid:[* TO *] AND !species_habitats:Marine AND geospatial_kosher:true AND (state:[* TO *] OR country:Australia)"
  val SPECIES_QUERY_TEMPLATE_NON_MARINE = "species_guid:{0} AND !species_habitats:Marine AND geospatial_kosher:true AND (state:[* TO *] OR country:Australia)"

  val SPECIES_FACET = "species_guid"
  val POINT_001_FACET = "point-0.001"

  def main(args: Array[String]) {
    val helper = new EndemismLayerHelper();
    //var allSpecies = false;
    var outputFileDirectory: String = null;
    var speciesCellCountsFilePrefix: String = null;
    var cellSpeciesFilePrefix: String = null;

    val parser = new OptionParser("Find expert distribution outliers") {
      arg("outputFileDirectory", "Directory in which to write the output files", {
        v: String => outputFileDirectory = v
      })
      arg("speciesCellCountsFilePrefix", "Prefix for files containing species cell counts (without .txt extension)", {
        v: String => speciesCellCountsFilePrefix = v
      })
      arg("cellSpeciesFilePrefix", "Prefix for files containing cell species lists (without .txt extension)", {
        v: String => cellSpeciesFilePrefix = v
      })
      //      booleanOpt("a", "allSpecies", "If true, endemism values will be calcuated for all species, instead of those that were recently updated.", {
      //        v: Boolean => allSpecies = v
      //      })
    }

    if (parser.parse(args)) {
      println("Output file directory: " + outputFileDirectory)
      println("Species cell counts file prefix: " + speciesCellCountsFilePrefix)
      println("Cell species file prefix: " + cellSpeciesFilePrefix)
      helper.calculateSpeciesEndemismValues(outputFileDirectory, speciesCellCountsFilePrefix, cellSpeciesFilePrefix)
    }
  }
}

class EndemismLayerHelper {

  val indexDAO = Config.indexDAO

  def calculateSpeciesEndemismValues(outputFileDirectory: String, speciesCellCountsFilePrefix: String, cellSpeciesFilePrefix: String) {
    // Data for all occurrences
    var cellSpeciesAll = scala.collection.mutable.Map[String, Set[String]]()
    var speciesCellCountsAll = scala.collection.mutable.Map[String, Int]()

    // Data for non-marine
    var cellSpeciesNonMarine = scala.collection.mutable.Map[String, Set[String]]()
    var speciesCellCountsNonMarine = scala.collection.mutable.Map[String, Int]()

    println("PROCESSING ALL OCCURRENCES")

    // get list of species for all occurrences
    val speciesLsidsAll = doFacetQuery(EndemismLayerHelper.ALL_SPECIES_QUERY, EndemismLayerHelper.SPECIES_FACET)

    for (lsid <- speciesLsidsAll) {
      println(lsid)
      val occurrencePoints = doFacetQuery(MessageFormat.format(EndemismLayerHelper.SPECIES_QUERY_TEMPLATE_ALL_OCCURRENCES, ClientUtils.escapeQueryChars(lsid)), EndemismLayerHelper.POINT_001_FACET)

      // process for 0.1 degree resolution
      processOccurrencePoints(occurrencePoints, lsid, cellSpeciesAll, speciesCellCountsAll, 1)
    }

    println("PROCESSING TERRESTRIAL (NON-MARINE) OCCURRENCES")

    // get list of species for marine occurrences only
    val speciesLsidsTerrestrialOnly = doFacetQuery(EndemismLayerHelper.NON_MARINE_QUERY, EndemismLayerHelper.SPECIES_FACET)

    for (lsid <- speciesLsidsTerrestrialOnly) {
      println(lsid)
      val occurrencePoints = doFacetQuery(MessageFormat.format(EndemismLayerHelper.SPECIES_QUERY_TEMPLATE_NON_MARINE, ClientUtils.escapeQueryChars(lsid)), EndemismLayerHelper.POINT_001_FACET)

      // process for 0.1 degree resolution
      processOccurrencePoints(occurrencePoints, lsid, cellSpeciesNonMarine, speciesCellCountsNonMarine, 1)
    }

    // write output for all occurrences
    val cellSpeciesFileAllOccurrences = outputFileDirectory + '/' + cellSpeciesFilePrefix + "-0.1-degree.txt"
    val speciesCellCountsFileAllOccurrences = outputFileDirectory + '/' + speciesCellCountsFilePrefix + "-0.1-degree.txt"
    writeFileOutput(cellSpeciesAll, speciesCellCountsAll, cellSpeciesFileAllOccurrences, speciesCellCountsFileAllOccurrences)

    //write output for terrestrial occurrences only
    val cellSpeciesFileNonMarine = outputFileDirectory + '/' + cellSpeciesFilePrefix + "-0.1-degree-non-marine.txt"
    val speciesCellCountsFileNonMarine = outputFileDirectory + '/' + speciesCellCountsFilePrefix + "-0.1-degree-non-marine.txt"
    writeFileOutput(cellSpeciesNonMarine, speciesCellCountsNonMarine, cellSpeciesFileNonMarine, speciesCellCountsFileNonMarine)

  }

  def processOccurrencePoints(occurrencePoints: ListBuffer[String], lsid: String, cellSpecies: scala.collection.mutable.Map[String, Set[String]], speciesCellCounts: scala.collection.mutable.Map[String, Int], numDecimalPlacesToRoundTo: Int) {
    var pointsSet = Set[String]()

    for (point <- occurrencePoints) {
      val splitPoint = point.split(",")
      val strLatitude = splitPoint(0)
      val strLongitude = splitPoint(1)

      val roundedLatitude = Precision.round(java.lang.Double.parseDouble(strLatitude), numDecimalPlacesToRoundTo, java.math.BigDecimal.ROUND_CEILING)
      val roundedLongitude = Precision.round(java.lang.Double.parseDouble(strLongitude), numDecimalPlacesToRoundTo, java.math.BigDecimal.ROUND_FLOOR)

      val strRoundedCoords = roundedLatitude + "," + roundedLongitude
      pointsSet += strRoundedCoords

      var thisCellSpecies = cellSpecies.getOrElse(strRoundedCoords, null)
      if (thisCellSpecies == null) {
        thisCellSpecies = Set[String]()
      }

      thisCellSpecies += lsid
      cellSpecies.put(strRoundedCoords, thisCellSpecies)
    }

    speciesCellCounts.put(lsid, pointsSet.size)
  }

  def writeFileOutput(cellSpecies: scala.collection.mutable.Map[String, Set[String]], speciesCellCounts: scala.collection.mutable.Map[String, Int], cellSpeciesFile: String, speciesCellCountsFile: String) {
    val bwSpeciesCellCounts = new BufferedWriter(new FileWriter(speciesCellCountsFile));
    for (lsid <- speciesCellCounts.keys) {
      bwSpeciesCellCounts.append(lsid)
      bwSpeciesCellCounts.append(",")
      bwSpeciesCellCounts.append(speciesCellCounts(lsid).toString)
      bwSpeciesCellCounts.newLine()
    }
    bwSpeciesCellCounts.flush()
    bwSpeciesCellCounts.close()

    val bwCellSpecies = new BufferedWriter(new FileWriter(cellSpeciesFile));
    for (cellCoords <- cellSpecies.keys) {
      bwCellSpecies.append(cellCoords)
      bwCellSpecies.append(",")
      bwCellSpecies.append(cellSpecies(cellCoords).mkString(","))
      bwCellSpecies.newLine()
    }
    bwCellSpecies.flush()
    bwCellSpecies.close()
  }

  def doFacetQuery(query: String, facet: String): ListBuffer[String] = {

    var resultsList = new ListBuffer[String]

    def addToList(name: String, count: Int): Boolean = {
      resultsList += name

      true
    }

    indexDAO.pageOverFacet(addToList, facet, query, Array())

    resultsList
  }

}
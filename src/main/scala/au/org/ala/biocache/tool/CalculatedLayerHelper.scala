package au.org.ala.biocache.tool

import collection.mutable.ListBuffer
import java.text.MessageFormat
import org.apache.commons.math3.util.Precision
import java.io.{BufferedWriter, FileWriter}
import org.apache.solr.client.solrj.util.ClientUtils
import actors.Actor
import actors.Actor._
import java.util.concurrent.CountDownLatch
import au.org.ala.biocache.Config
import au.org.ala.biocache.util.OptionParser

/**
 * Created with IntelliJ IDEA.
 * User: ChrisF
 * Date: 29/11/12
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
object CalculatedLayerHelper {

  //val FACET_DOWNLOAD_URL_TEMPLATE = Config.biocacheServiceUrl + "/occurrences/facets/download?q={0}&facets={1}"
  //val FACET_DOWNLOAD_URL_TEMPLATE = "http://ala-rufus.it.csiro.au/biocache-service/occurrences/facets/download?q={0}&facets={1}"

  // All occurrences
  val SPECIES_GUID_QUERY = "species_guid:*"
  val SPECIFIC_SPECIES_GUID_QUERY_TEMPLATE = "species_guid:{0}"

  // All occurrences
  val ALL_SPECIES_FILTER_QUERY = "geospatial_kosher:true"

  // Terrestrial (non-marine) occurrences
  val NON_MARINE_SPECIES_FILTER_QUERY = "!species_habitats:Marine AND geospatial_kosher:true AND (state:* OR country:Australia)"

  val SPECIES_FACET = "species_guid"
  val POINT_001_FACET = "point-0.001"

  def main(args: Array[String]) {
    val helper = new CalculatedLayerHelper();
    var outputFileDirectory: String = null;
    var speciesCellCountsFilePrefix: String = null;
    var cellSpeciesFilePrefix: String = null;
    var cellOccurrenceCountsFilePrefix: String = null;
    var numThreads = 1;

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
      arg("cellOccurrenceCountsFilePrefix", "Prefix for files containing cell occurrence counts (without .txt extension)", {
        v: String => cellOccurrenceCountsFilePrefix = v
      })
      intOpt("t", "numThreads", "Number of threads to use", {
        v: Int => numThreads = v
      })
    }

    if (parser.parse(args)) {
      println("Output file directory: " + outputFileDirectory)
      println("Species cell counts file prefix: " + speciesCellCountsFilePrefix)
      println("Cell species file prefix: " + cellSpeciesFilePrefix)
      println("Cell occurrence counts file prefix: " + cellOccurrenceCountsFilePrefix)
      helper.execute(numThreads, outputFileDirectory, speciesCellCountsFilePrefix, cellSpeciesFilePrefix, cellOccurrenceCountsFilePrefix)
    }
  }

  def doFacetQuery(query: String, filterQuery: String, facet: String): (ListBuffer[String], scala.collection.mutable.Map[String, Int]) = {

    var valuesList = new ListBuffer[String]
    val valuesCountMap = scala.collection.mutable.Map[String, Int]()

    def addToList(name: String, count: Int): Boolean = {
      valuesList += name
      valuesCountMap.put(name, count)

      true
    }

    Config.indexDAO.pageOverFacet(addToList, facet, query, Array(filterQuery))

    (valuesList, valuesCountMap)
  }
}

class CalculatedLayerHelper {

  def execute(numThreads: Int, outputFileDirectory: String, speciesCellCountsFilePrefix: String, cellSpeciesFilePrefix: String, cellOccurrenceCountsFilePrefix: String) {
    println("PROCESSING TERRESTRIAL (NON-MARINE) OCCURRENCES (0.1 degree)")
    calculateCellValues(numThreads, outputFileDirectory, speciesCellCountsFilePrefix + "-0.1-degree-non-marine", cellSpeciesFilePrefix + "-0.1-degree-non-marine", cellOccurrenceCountsFilePrefix + "-0.1-degree-non-marine", CalculatedLayerHelper.NON_MARINE_SPECIES_FILTER_QUERY, 1)

    println("PROCESSING ALL OCCURRENCES (0.1 degree)")
    calculateCellValues(numThreads, outputFileDirectory, speciesCellCountsFilePrefix + "-0.1-degree", cellSpeciesFilePrefix + "-0.1-degree", cellOccurrenceCountsFilePrefix + "-0.1-degree", CalculatedLayerHelper.ALL_SPECIES_FILTER_QUERY, 1)

    println("PROCESSING ALL OCCURRENCES (0.01 degree)")
    calculateCellValues(numThreads, outputFileDirectory, speciesCellCountsFilePrefix + "-0.01-degree", cellSpeciesFilePrefix + "-0.01-degree", cellOccurrenceCountsFilePrefix + "-0.01-degree", CalculatedLayerHelper.ALL_SPECIES_FILTER_QUERY, 2)
  }

  def calculateCellValues(numThreads: Int, outputFileDirectory: String, speciesCellCountsFilePrefix: String, cellSpeciesFilePrefix: String, cellOccurrenceCountsFilePrefix: String, filterQuery: String, numDecimalPlacesToRoundTo:Int) {

    val countDownLatch = new CountDownLatch(numThreads);

    //Dispatcher actor
    actor {
      var cellSpecies = scala.collection.mutable.Map[String, Set[String]]()
      var speciesCellCounts = scala.collection.mutable.Map[String, Int]()
      var cellOccurrenceCounts = scala.collection.mutable.Map[String, Int]()

      val cellSpeciesFile = outputFileDirectory + '/' + cellSpeciesFilePrefix + ".txt"
      val speciesCellCountsFile = outputFileDirectory + '/' + speciesCellCountsFilePrefix + ".txt"
      val cellOccurrenceCountsFile = outputFileDirectory + '/' + cellOccurrenceCountsFilePrefix + ".txt"

      val (speciesLsids, speciesOccurrenceCounts) = CalculatedLayerHelper.doFacetQuery(CalculatedLayerHelper.SPECIES_GUID_QUERY, filterQuery, CalculatedLayerHelper.SPECIES_FACET)

      var workQueue = scala.collection.mutable.Queue[String]()
      workQueue ++= speciesLsids

      for (i <- 0 to numThreads - 1) {
        val a = new CalculatedLayerWorkerActor(i, filterQuery, numDecimalPlacesToRoundTo, self)
        a.start()
      }

      var completedThreads = 0
      loopWhile(completedThreads < numThreads) {
        receive {
          case ("SEND JOB", actor: CalculatedLayerWorkerActor) => {
            if (!workQueue.isEmpty) {
              val lsid = workQueue.dequeue
              actor ! (lsid)
            } else {
              actor ! "EXIT"
            }
          }

          case ("EXITED", actor: CalculatedLayerWorkerActor) => {
            // merge the worker's speciesCellCounts into the global species cell counts
            for (lsid <- actor.speciesCellCounts.keys) {
              speciesCellCounts.put(lsid, actor.speciesCellCounts(lsid))
            }

            // merge the worker's cellSpecies lists into the global cell species lists
            for (coords <- actor.cellSpecies.keys) {
              val actorCellSpecies = actor.cellSpecies(coords)
              if (cellSpecies.contains(coords)) {
                var globalCellSpecies = cellSpecies(coords)
                globalCellSpecies ++= actorCellSpecies
                cellSpecies.put(coords, globalCellSpecies)
              } else {
                cellSpecies.put(coords, actorCellSpecies)
              }
            }

            // merge the worker's cellOccurrenceCounts into the global cell occurrenceCounts
            for (coords <- actor.cellOccurrenceCounts.keys) {
              val actorCellOccurrenceCount = actor.cellOccurrenceCounts(coords)
              if (cellOccurrenceCounts.contains(coords)) {
                var globalCellOccurrenceCount = cellOccurrenceCounts(coords)
                globalCellOccurrenceCount = globalCellOccurrenceCount + actorCellOccurrenceCount
                cellOccurrenceCounts.put(coords, globalCellOccurrenceCount)
              } else {
                cellOccurrenceCounts.put(coords, actorCellOccurrenceCount)
              }
            }

            completedThreads += 1

            //If all threads have exited, then write file output
            if (completedThreads == numThreads) {
              writeFileOutput(cellSpecies, speciesCellCounts, cellOccurrenceCounts, cellSpeciesFile, speciesCellCountsFile, cellOccurrenceCountsFile)
            }

            countDownLatch.countDown()
          }

          case msg => Console.err.println("received message " + msg)
        }
      }
    }

    // Calling thread must wait until the dispatcher actor has exited
    countDownLatch.await()
  }

  def writeFileOutput(cellSpecies: scala.collection.mutable.Map[String, Set[String]], speciesCellCounts: scala.collection.mutable.Map[String, Int], cellOccurrenceCounts: scala.collection.mutable.Map[String, Int], cellSpeciesFile: String, speciesCellCountsFile: String, cellOccurrenceCountsFile: String) {
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

    val bwCellOccurrenceCounts = new BufferedWriter(new FileWriter(cellOccurrenceCountsFile));
    for (cellCoords <- cellOccurrenceCounts.keys) {
      bwCellOccurrenceCounts.append(cellCoords)
      bwCellOccurrenceCounts.append(",")
      bwCellOccurrenceCounts.append(cellOccurrenceCounts(cellCoords).toString)
      bwCellOccurrenceCounts.newLine()
    }
    bwCellOccurrenceCounts.flush()
    bwCellOccurrenceCounts.close()
  }
}

class CalculatedLayerWorkerActor(id: Int, filterQuery: String, numDecimalPlacesToRoundTo: Int, dispatcher: Actor) extends Actor {

  var cellSpecies = scala.collection.mutable.Map[String, Set[String]]()
  var speciesCellCounts = scala.collection.mutable.Map[String, Int]()
  var cellOccurrenceCounts = scala.collection.mutable.Map[String, Int]()

  def act() {
    println("Worker(" + id + ") started")

    // Ask the dispatcher for an initial job
    dispatcher !("SEND JOB", self)

    loop {
      react {
        case "EXIT" => {
          println("Worker(" + id + ") stopping")
          dispatcher !("EXITED", self)
          exit()
        }
        case (speciesLsid: String) => {
          println("Worker(" + id + ") processing " + speciesLsid)
          val (speciesPoints, speciesPointOccurrenceCounts)  = CalculatedLayerHelper.doFacetQuery(MessageFormat.format(CalculatedLayerHelper.SPECIFIC_SPECIES_GUID_QUERY_TEMPLATE, ClientUtils.escapeQueryChars(speciesLsid)), filterQuery, CalculatedLayerHelper.POINT_001_FACET)

          processSpeciesOccurrences(speciesLsid, speciesPoints, speciesPointOccurrenceCounts, cellSpecies, speciesCellCounts, cellOccurrenceCounts, numDecimalPlacesToRoundTo) // 1 decimal place for 0.1 degree resolution, 2 decimal places for 0.01 degree resolution etc.

          // ask dispatcher for next job
          dispatcher !("SEND JOB", self)
        }
      }
    }
  }

  def processSpeciesOccurrences(speciesLsid: String, speciesPoints: ListBuffer[String], speciesPointOccurrenceCounts: scala.collection.mutable.Map[String, Int], cellSpecies: scala.collection.mutable.Map[String, Set[String]], speciesCellCounts: scala.collection.mutable.Map[String, Int], cellOccurrenceCounts: scala.collection.mutable.Map[String, Int], numDecimalPlacesToRoundTo: Int) {
    var pointsSet = Set[String]()

    for (point <- speciesPoints) {
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

      thisCellSpecies += speciesLsid
      cellSpecies.put(strRoundedCoords, thisCellSpecies)

      val speciesPointOccurrenceCount = speciesPointOccurrenceCounts.getOrElse(point, 0)
      var thisCellOccurrenceCount = cellOccurrenceCounts.getOrElse(strRoundedCoords, 0)

      thisCellOccurrenceCount = thisCellOccurrenceCount + speciesPointOccurrenceCount
      cellOccurrenceCounts.put(strRoundedCoords, thisCellOccurrenceCount)
    }

    speciesCellCounts.put(speciesLsid, pointsSet.size)
  }
}
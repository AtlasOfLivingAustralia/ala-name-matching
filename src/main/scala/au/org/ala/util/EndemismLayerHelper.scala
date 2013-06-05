package au.org.ala.util

import collection.mutable.ListBuffer
import java.text.MessageFormat
import au.org.ala.biocache.Config
import org.apache.commons.math3.util.Precision
import java.io.{BufferedWriter, FileWriter}
import org.apache.solr.client.solrj.util.ClientUtils
import actors.Actor
import actors.Actor._
import java.util.concurrent.CountDownLatch
import org.apache.commons.codec.net.URLCodec
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.io.IOUtils

/**
 * Created with IntelliJ IDEA.
 * User: ChrisF
 * Date: 29/11/12
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
object EndemismLayerHelper {
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
    val helper = new EndemismLayerHelper();
    var outputFileDirectory: String = null;
    var speciesCellCountsFilePrefix: String = null;
    var cellSpeciesFilePrefix: String = null;
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
      intOpt("t", "numThreads", "Number of threads to use", {
        v: Int => numThreads = v
      })
    }

    if (parser.parse(args)) {
      println("Output file directory: " + outputFileDirectory)
      println("Species cell counts file prefix: " + speciesCellCountsFilePrefix)
      println("Cell species file prefix: " + cellSpeciesFilePrefix)
      helper.execute(numThreads, outputFileDirectory, speciesCellCountsFilePrefix, cellSpeciesFilePrefix)
    }
  }

  def doFacetQuery(query: String, filterQuery: String, facet: String): ListBuffer[String] = {

    var resultsList = new ListBuffer[String]

    def addToList(name: String, count: Int): Boolean = {
      resultsList += name

      true
    }

    Config.indexDAO.pageOverFacet(addToList, facet, query, Array(filterQuery))

    resultsList
  }
}

class EndemismLayerHelper {

  def execute(numThreads: Int, outputFileDirectory: String, speciesCellCountsFilePrefix: String, cellSpeciesFilePrefix: String) {
    println("PROCESSING TERRESTRIAL (NON-MARINE) OCCURRENCES")
    calculateSpeciesEndemismValues(numThreads, outputFileDirectory, speciesCellCountsFilePrefix + "-0.1-degree-non-marine", cellSpeciesFilePrefix + "-0.1-degree-non-marine", EndemismLayerHelper.NON_MARINE_SPECIES_FILTER_QUERY)

    println("PROCESSING ALL OCCURRENCES")
    calculateSpeciesEndemismValues(numThreads, outputFileDirectory, speciesCellCountsFilePrefix + "-0.1-degree", cellSpeciesFilePrefix + "-0.1-degree", EndemismLayerHelper.ALL_SPECIES_FILTER_QUERY)
  }

  def calculateSpeciesEndemismValues(numThreads: Int, outputFileDirectory: String, speciesCellCountsFilePrefix: String, cellSpeciesFilePrefix: String, filterQuery: String) {

    val countDownLatch = new CountDownLatch(numThreads);

    //Dispatcher actor
    actor {
      var cellSpecies = scala.collection.mutable.Map[String, Set[String]]()
      var speciesCellCounts = scala.collection.mutable.Map[String, Int]()

      val cellSpeciesFile = outputFileDirectory + '/' + cellSpeciesFilePrefix + ".txt"
      val speciesCellCountsFile = outputFileDirectory + '/' + speciesCellCountsFilePrefix + ".txt"

      val speciesLsids = EndemismLayerHelper.doFacetQuery(EndemismLayerHelper.SPECIES_GUID_QUERY, filterQuery, EndemismLayerHelper.SPECIES_FACET)
//      var speciesLsids = ListBuffer[String]()
//      speciesLsids += "urn:lsid:biodiversity.org.au:afd.taxon:b76f8dcf-fabd-4e48-939c-fd3cafc1887a"
//      speciesLsids += "urn:lsid:biodiversity.org.au:afd.taxon:eb00b378-cb04-4abc-bdca-295ddd52ada9"

      var workQueue = scala.collection.mutable.Queue[String]()
      workQueue ++= speciesLsids

      for (i <- 0 to numThreads - 1) {
        val a = new EndemismWorkerActor(i, filterQuery, self)
        a.start()
      }

      var completedThreads = 0
      loopWhile(completedThreads < numThreads) {
        receive {
          case ("SEND JOB", actor: EndemismWorkerActor) => {
            if (!workQueue.isEmpty) {
              val lsid = workQueue.dequeue
              actor ! (lsid)
            } else {
              actor ! "EXIT"
            }
          }

          case ("EXITED", actor: EndemismWorkerActor) => {
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

            completedThreads += 1

            //If all threads have exited, then write file output
            if (completedThreads == numThreads) {
              writeFileOutput(cellSpecies, speciesCellCounts, cellSpeciesFile, speciesCellCountsFile)
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
}

class EndemismWorkerActor(id: Int, filterQuery: String, dispatcher: Actor) extends Actor {

  var cellSpecies = scala.collection.mutable.Map[String, Set[String]]()
  var speciesCellCounts = scala.collection.mutable.Map[String, Int]()

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
          val occurrencePoints = EndemismLayerHelper.doFacetQuery(MessageFormat.format(EndemismLayerHelper.SPECIFIC_SPECIES_GUID_QUERY_TEMPLATE, ClientUtils.escapeQueryChars(speciesLsid)), filterQuery, EndemismLayerHelper.POINT_001_FACET)

          // process for 0.1 degree resolution
          processOccurrencePoints(occurrencePoints, speciesLsid, cellSpecies, speciesCellCounts, 1)

          // ask dispatcher for next job
          dispatcher !("SEND JOB", self)
        }
      }
    }
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
}
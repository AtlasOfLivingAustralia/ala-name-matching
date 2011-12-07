package au.org.ala.biocache.outliers
import java.net.URL
import java.io.FileOutputStream
import java.io.File
import au.org.ala.util.FileHelper
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.mutable.ListBuffer
import org.ala.layers.intersect.Grid

object CellIDLookupTest {

  def main(args: Array[String]) {
    val grid = new Grid("/data/ala/data/layers/ready/diva/bioclim_bio17")
    println(SpeciesOutlierTests.getCellId(145.2717, -16.58889, grid.xmin, grid.xmax, grid.xres, grid.ymin, grid.ymax, grid.yres, grid.nrows, grid.ncols))
    println(SpeciesOutlierTests.getCellId(145.271, -16.5888, grid.xmin, grid.xmax, grid.xres, grid.ymin, grid.ymax, grid.yres, grid.nrows, grid.ncols))
    println(SpeciesOutlierTests.getCellId(145.27, -16.588, grid.xmin, grid.xmax, grid.xres, grid.ymin, grid.ymax, grid.yres, grid.nrows, grid.ncols))
    println(SpeciesOutlierTests.getCellId(145.27, -16.58, grid.xmin, grid.xmax, grid.xres, grid.ymin, grid.ymax, grid.yres, grid.nrows, grid.ncols))
    println(SpeciesOutlierTests.getCellId(145.268, -16.58, grid.xmin, grid.xmax, grid.xres, grid.ymin, grid.ymax, grid.yres, grid.nrows, grid.ncols))
  }
}

object SpeciesOutlierTests {

  import FileHelper._

  val map = Map(
    "el882" -> "/data/ala/data/layers/ready/diva/bioclim_bio15",
    "el889" -> "/data/ala/data/layers/ready/diva/bioclim_bio17",
    "el887" -> "/data/ala/data/layers/ready/diva/bioclim_bio23",
    "el865" -> "/data/ala/data/layers/ready/diva/bioclim_bio26",
    "el894" -> "/data/ala/data/layers/ready/diva/bioclim_bio32")

  def main(args: Array[String]) {

    val lsid = "urn:lsid:biodiversity.org.au:afd.taxon:0c139726-2add-4abe-a714-df67b1d4b814"

    //val requiredFields = Array("id","latitude", "longitude", "el889", "el882", "el887", "el865", "el894")
    val variablesToTest = Array("el889", "el882", "el887", "el865", "el894")
    val requiredFields = Array("id", "latitude", "longitude") ++ variablesToTest

    val u = new URL("http://biocache.ala.org.au/ws/webportal/occurrences.gz?pageSize=3000000&q=lsid:\"" + lsid + "\"&fl=" + requiredFields.mkString(","))

    val in = u.openStream
    val file = new File("/tmp/occurrences.gz")
    val out = new FileOutputStream(file)
    val buffer: Array[Byte] = new Array[Byte](1024)
    var numRead = 0
    while ({ numRead = in.read(buffer); numRead != -1 }) {
      out.write(buffer, 0, numRead)
      out.flush
    }
    out.close()
    printf("\nDownloaded. File size: ", file.length() / 1024 + "kB, " + file.getAbsolutePath)

    //decompress
    val extractedFile = file.extractGzip
    //read the CSV
    //val outlierValues = new scala.collection.mutable.HashMap[String,List[String]]

    //outlier values for each layer 
    val outlierValues: Array[(String, List[String])] = variablesToTest.map(variable => {
      println("************ test with " + variable)
      val outliers = performJacknife(variable, extractedFile)
      variable -> outliers
    })

    val recordLayerCounts = filterOutliersForXLayers(outlierValues)
    val outlier5 = recordLayerCounts.filter(x => x._2 == 5).map(x => x._1).sorted
    val outlier4 = recordLayerCounts.filter(x => x._2 == 4).map(x => x._1).sorted
    val outlier3 = recordLayerCounts.filter(x => x._2 == 3).map(x => x._1).sorted
    val outlier2 = recordLayerCounts.filter(x => x._2 == 2).map(x => x._1).sorted
    val outlier1 = recordLayerCounts.filter(x => x._2 == 1).map(x => x._1).sorted

    printLinks(5, outlier5)
    printLinks(4, outlier4)
    printLinks(3, outlier3)
    printLinks(2, outlier2)
    printLinks(1, outlier1)

  }

  def printLinks(count: Int, records: List[String]) {
    println()
    println("************************ outlier for : " + count + " ******************")
    records.foreach(c => println("http://biocache.ala.org.au/occurrences/" + c))
    println("************************ end of outlier for : " + count + " ******************")
    println()
  }

  def filterOutliersForXLayers(outliersMap: Array[(String, List[String])]): List[(String, Int)] = {
    //convert the map into a map keyed on recordID
    val recordIds = outliersMap.map(a => a._2).flatten.toList.distinct
    val counts = Array.fill(recordIds.size)(0)
    outliersMap.foreach(layerId2Records => {
      layerId2Records._2.foreach(recordId => {
        val recordIdx = recordIds.indexOf(recordId)
        counts(recordIdx) = counts(recordIdx) + 1
      })
    })
    recordIds zip counts
  }

  def performJacknife(columnName: String, file: File): List[String] = {
    val r = new CSVReader(new FileReader(file))
    val headers = r.readNext().toList
    val grid = new Grid(map(columnName))

    val points = new ListBuffer[Point]
    var line = r.readNext()
    while (line != null) {

      val fields = (headers zip line).toMap

      //println(fields)
      if (fields.contains("latitude") && fields("latitude") != "" && fields.getOrElse(columnName, "") != "") {
        val id = fields("id")
        val x = fields("longitude").toFloat
        val y = fields("latitude").toFloat
        val elValue = fields(columnName).toFloat

        points += Point(id, y, x, elValue,
          getCellId(x.toDouble, y.toDouble, grid.xmin, grid.xmax, grid.xres, grid.ymin, grid.ymax, grid.yres, grid.nrows, grid.ncols))
      }
      line = r.readNext()
    }

    //we have a points, group by on cellId as we only want to sample once per cell
    val pointsGroupedByCell = points.groupBy(p => p.cellId)

    println("record_id,latitude,longitude,derivedValue for"+ columnName + ", cell id")
    pointsGroupedByCell.foreach(pgc => {
      val firstPointForGroup = pgc._2.first
      println(firstPointForGroup.id+","+firstPointForGroup.lat+","+firstPointForGroup.lon+","+firstPointForGroup.value+","+firstPointForGroup.cellId)
    })
    
    //create a cell -> value map
    val cellToValue = pointsGroupedByCell.map(g => g._1 -> g._2.first.value).toMap[Int, Float]

    //create a value -> cell map    
    val valuesToCells = cellToValue.groupBy(x => x._2).map(x => x._1 -> x._2.keys.toSet[Int]).toMap

    //the environmental properties to throw at Jackknife test
    val valuesToTest = cellToValue.values.filter(x => x != "").map(y => y.toFloat).toList

    r.close

    //do jacknife test
    val valuesConsideredOutliers = JackKnife.jackknife(valuesToTest)

    val outliers = new ListBuffer[String]
    valuesConsideredOutliers.get.map(x => {
      //get the cell
      val cellIds = valuesToCells.getOrElse(x, Set())
      cellIds.foreach(cellId => {
        val points = pointsGroupedByCell.get(cellId).get
        points.foreach(p => {
          println("*********** marked as an outlier: " + p.id)
          outliers += p.id
        })
      })
    })

    outliers.toList
  }

  def getCellId(x: Double, y: Double, xmin: Double, xmax: Double, xres: Double, ymin: Double, ymax: Double, yres: Double, nrows: Int, ncols: Int): Int = {
    //handle invalid inputs
    if (x < xmin || x > xmax || y < ymin || y > ymax) {
      -1
    } else {

      var col = ((x - xmin) / xres).toInt
      var row = nrows - 1 - ((y - ymin) / yres).toInt

      //limit each to 0 and ncols-1/nrows-1
      if (col < 0) {
        col = 0
      }
      if (row < 0) {
        row = 0
      }
      if (col >= ncols) {
        col = ncols - 1;
      }
      if (row >= nrows) {
        row = nrows - 1;
      }

      row * ncols + col
    }
  }
}

case class Point(id: String, lat: Float, lon: Float, value: Float, cellId: Int)
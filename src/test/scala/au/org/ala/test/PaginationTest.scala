package au.org.ala.test
import java.io.{ File }
import java.util.ArrayList
import org.apache.cassandra.thrift._
import org.wyki.cassandra.pelops.{ Selector, Policy, Pelops }
import scala.collection.immutable.Set
import scala.reflect._

object PaginationTest {

  def main(args: Array[String]): Unit = {
    val hosts = Array { "localhost" }
    Pelops.addPool("test-pool", hosts, 9160, false, "occ", new Policy());
    val selector = Pelops.createSelector("test-pool", "occ");
    val slicePredicate = new SlicePredicate

    val columnNames = new ArrayList[Array[Byte]]
    columnNames.add("key".getBytes)
    slicePredicate.setColumn_names(columnNames)

    // page through all dr1 style keys
    var startKey = ""
    var keyRange = Selector.newKeyRange("dr3@", "", 101)

    val columnMap = selector.getColumnsFromRows(keyRange, "dr", slicePredicate, ConsistencyLevel.ONE)

    if (columnMap.size > 0) {
      //get the array of keys
      val columnsObj = List(columnMap.keySet.toArray: _*)
      val columns = columnsObj.asInstanceOf[List[String]]
      //print last
      for (key <- columns) {

        if (key.startsWith("dr3@")) {
          println(key)
        }
      }

      keyRange = Selector.newKeyRange(startKey, "", 101)
    }

    Pelops.shutdown
  }
}
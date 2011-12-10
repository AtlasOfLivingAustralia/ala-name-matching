package au.org.ala.util

import org.apache.cassandra.thrift.ConsistencyLevel
import org.apache.cassandra.thrift.{SlicePredicate, Column, ConsistencyLevel, IndexOperator}
import org.scale7.cassandra.pelops.{Cluster,Pelops,Selector, Bytes}

object GetKeyPartitions {

  def main(args:Array[String]){

      val pageSize = 10000
      val cluster = new Cluster("localhost",9160)
      Pelops.addPool("ALA-Partition-Calculator", cluster, "occ")
      val selector = Pelops.createSelector("ALA-Partition-Calculator")
      var startKey = new Bytes("".getBytes)
      var endKey = new Bytes("".getBytes)
      val slicePredicate = Selector.newColumnsPredicate(Array[String]():_*)
      var keyRange = Selector.newKeyRange(startKey, endKey, pageSize + 1)
      var hasMore = true
      var counter = 0
      //Please note we are not paging by UTF8 because it is much slower
      var columnMap = selector.getColumnsFromRows("occ", keyRange, slicePredicate, ConsistencyLevel.ONE)
      var continue = true
      while (!columnMap.isEmpty && continue) {
        val columnsObj = List(columnMap.keySet.toArray : _*)
        //convert to scala List
        val keys = columnsObj.asInstanceOf[List[Bytes]]
        startKey = keys.last
        println("Start key = " + startKey.toUTF8)
        counter += keys.size
        keyRange = Selector.newKeyRange(startKey, endKey, pageSize+1)
        columnMap = selector.getColumnsFromRows("occ", keyRange, slicePredicate, ConsistencyLevel.ONE)
        columnMap.remove(startKey)
      }
      if(counter > 0) println("Finished paging. Total count: "+counter)
  }
}
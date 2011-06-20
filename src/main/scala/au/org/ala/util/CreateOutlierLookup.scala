package au.org.ala.util

import au.org.ala.biocache.{Config, CassandraPersistenceManager}
import org.apache.cassandra.thrift.{ConsistencyLevel, InvalidRequestException, CfDef}
import collection.JavaConversions
import org.scale7.cassandra.pelops.{Bytes, Selector, Pelops}

object CreateOutlierLookup {

    import JavaConversions._

    def main(args:Array[String]){

      //retrieve all the records
      val pm:CassandraPersistenceManager = Config.persistenceManager.asInstanceOf[CassandraPersistenceManager]
      val columnFamilyManager = Pelops.createColumnFamilyManager(pm.cluster, "occ")

      try {
        var cdef = new CfDef("occ", "outlier_lookup")
        cdef.column_type = "Standard"
        cdef.comparator_type = "UTF8Type"
        cdef.default_validation_class = "UTF8Type"
        columnFamilyManager.addColumnFamily(cdef)
      } catch {
        case e:InvalidRequestException => columnFamilyManager.truncateColumnFamily("outlier_lookup")
      }

      pm.pageOverAll("outliers", (guid, map) => {

        val (taxonConceptID, recordID) = {
            val values = guid.split("\\|\\|")
            (values.head, values.last)
        }

        //retrieve the lat long
        val selector =  Pelops.createSelector(pm.poolName)
        val colsPredicate = Selector.newColumnsPredicate(Array("decimalLatitude", "decimalLongitude"):_*)
        val cols = selector.getColumnsFromRow("occ", recordID, colsPredicate, ConsistencyLevel.ONE)
        val fieldValues = cols.map(column => (new String(column.getName, "UTF-8"),new String(column.getValue, "UTF-8"))).toMap

        //add a row to the outlier_lookup
        println(guid)
        if(fieldValues.containsKey("decimalLatitude") && fieldValues.containsKey("decimalLongitude")){

            val outlierLookupGuid = {
                  taxonConceptID + "||" + fieldValues("decimalLatitude") + "||" + fieldValues("decimalLongitude")
            }
            val mutator = Pelops.createMutator(pm.poolName)
            mutator.writeColumn("outlier_lookup", outlierLookupGuid, mutator.newColumn(recordID, "0"))
    //        map.foreach( {case (key, value) =>
    //            mutator.writeColumn("outlier_lookup", outlierLookupGuid, mutator.newColumn(key, value))
    //        } )
            mutator.execute(ConsistencyLevel.ONE)
        }
        true
      })
    }
}
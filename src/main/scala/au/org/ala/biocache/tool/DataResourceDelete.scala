package au.org.ala.biocache.tool

import au.org.ala.biocache.util.RecordDeletor

/**
 * A utility to delete a data resource
 */
class DataResourceDelete(dataResource:String) extends RecordDeletor {

    override def deleteFromPersistent {
        //page over all the records for the data resource deleting them
        var count = 0
        val start = System.currentTimeMillis
        val startUuid = dataResource +"|"
        val endUuid = startUuid + "~"

        pm.pageOverSelect("occ", (guid,map)=>{
//            pm.delete(guid, "occ")
            //use the occ DAO to delete so that the record is added to the dellog cf
            occurrenceDAO.delete(guid,false,true)
            count= count +1
            true
        }, startUuid, endUuid, 1000, "rowKey", "uuid")
        val finished = System.currentTimeMillis

      println("Deleted " + count + " records in "  + (finished -start).toFloat / 60000f + " minutes.")
    }
    override def deleteFromIndex {
        indexer.removeByQuery("data_resource_uid:" +dataResource)
    }
}

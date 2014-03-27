package au.org.ala.biocache.tool

import au.org.ala.biocache.util.RecordDeletor
import au.org.ala.biocache.load.FullRecordMapper

/**
 * A deletor that marks occurrence records, for a specific data resource, as deleted before
 * removing them at a later time.
 */
class DataResourceVirtualDelete(dataResource:String) extends RecordDeletor {

    override def deleteFromPersistent {
        var count = 0
        val start = System.currentTimeMillis
        val startUuid = dataResource +"|"
        val endUuid = startUuid + "~"

        pm.pageOverSelect("occ", (guid,map)=>{
            occurrenceDAO.setDeleted(guid, true)
            count= count +1
            true
        }, startUuid, endUuid, 1000, "rowKey", "uuid")
        val finished = System.currentTimeMillis

      println("Marked " + count + " records as deleted in "  + (finished -start).toFloat / 60000f + " minutes.")
    }

    override def deleteFromIndex = indexer.removeByQuery("data_resource_uid:" + dataResource)

    /**
     * Physically deletes all records where deleted=true in the persistence manager.
     */
    def physicallyDeleteMarkedRecords {
        var count = 0
        val start = System.currentTimeMillis
        val startUuid = dataResource +"|"
        val endUuid = startUuid + "~"
        pm.pageOverSelect("occ", (guid,map)=>{
            val delete = map.getOrElse(FullRecordMapper.deletedColumn, "false")
            if("true".equals(delete)){
                //pm.delete(guid, "occ")
                //use the occ DAO to delete so that the record is added to the dellog cf
                occurrenceDAO.delete(guid,false,true)
                count= count +1
            }
            true
        }, startUuid, endUuid, 1000, "rowKey", "uuid", FullRecordMapper.deletedColumn)
        val finished = System.currentTimeMillis

      println("Deleted " + count + " records in "  + (finished -start).toFloat / 60000f + " minutes.")
    }
}

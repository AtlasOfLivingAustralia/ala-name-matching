package au.org.ala.util

import org.apache.cassandra.db.{RowMutation, ColumnFamily}
import org.apache.cassandra.service.{StorageProxy, StorageService}
import org.apache.cassandra.db.filter.QueryPath
import org.apache.cassandra.thrift.ThriftGlue.createColumnPath
import org.gbif.dwc.terms.DwcTerm
import org.gbif.dwc.text.ArchiveFactory
import java.io.File
import java.util.{UUID, Arrays}
import org.wyki.cassandra.pelops.Pelops
import au.org.ala.biocache._
import collection.JavaConversions

/**
 * A dwc loader that uses native APIs. This requires that cassandra is stopped.
 * This needs to be ran in two separate consecutive phases/processes.
 * 1) Initial loading
 * 2) Population of dr column family with GUID
 *
 * This can only be ran on the first load of a dataset.
 */
object FastDwCLoader {

    import JavaConversions._
    import scalaj.collection.Imports._
    import ReflectBean._

    def main(args:Array[String]){
        
        if(args.length==2){
            println("Starting loading with native API....")
            init(args(0))     //   /Users/davejmartin2/dev/biocache-store/conf
            loadWithNativeAPIs(args(1))       //"/data/biocache/ozcam/"
            shutdown
            println("Loading finished. Kill -9 this process")
            exit(1)
        }
        
        if(args.length==1){
            println("loading the UUID mapping")
            retrofitUUIDMapping(args(0))
            Pelops.shutdown
            println("DONE")
            exit(1)
        }
    }

    def init(storageConfFilePath:String) {
        //storageConfFilePath =  /Users/davejmartin2/dev/biocache-store/conf
        System.setProperty("storage-config", storageConfFilePath)
        //initialise bulk load API
        StorageService.instance.initClient();
    }

    def shutdown {
        //shutdown
        StorageService.instance.drain
        StorageService.instance.forceTableFlush("occ", "occ")
        StorageService.instance.forceTableCompaction
        System.out.println("Done writing.");
        StorageService.instance.stopClient();
    }

    def retrofitUUIDMapping(dataResourceUID:String) {

        var counter = 0
        
        OccurrenceDAO.pageOverAll(Versions.RAW, record => {
            val uniqueID = dataResourceUID+"|"+record.get.occurrence.institutionCode+"|"+record.get.occurrence.collectionCode +"|"+record.get.occurrence.catalogNumber
            DAO.persistentManager.put(uniqueID, "dr", "uuid", record.get.uuid)
            counter+=1
            if(counter % 1000 == 0){
              System.out.println(counter + " >> update mapping for: " + record.get.uuid+", uniqueID: "+uniqueID);
            }
            true
        })
    }


    def loadWithNativeAPIs(filePath:String) {

//        System.setProperty("storage-config", "/Users/davejmartin2/dev/biocache-store/conf")
//        //initialise bulk load API
//        StorageService.instance.initClient();
        val comp = ColumnFamily.getComparatorFor("occ", "occ", null);

        //initialise DWC
        val file = new File(filePath)
        val archive = ArchiveFactory.openArchive(file)
        val iter = archive.iteratorDwc
        val terms = DwcTerm.values

        var counter = 0
        while (iter.hasNext) {
        //for (i <- 1 to 1000) {
          counter+=1
          val dwc = iter.next
          //create a map of properties
          val fieldTuples:Array[(String,String)] = {
            (for {
              term <- terms
              val property = dwc.getterWithOption(term.simpleName)
              if (!property.isEmpty && property.get.toString.trim.length>0)
            } yield (term.simpleName -> property.get.toString))
          }

          val newUuid = UUID.randomUUID.toString
          var change = new RowMutation("occ", newUuid);

          for (tuple <- fieldTuples){
            var cp = createColumnPath("occ", null, (tuple._1).getBytes());
            change.add(new QueryPath(cp), (tuple._2).getBytes(), 0);
          }

          // don't call change.apply().  The reason is that is makes a static call into Table, which will perform
          // local storage initialization, which creates local directories.
          change.apply();

          StorageProxy.mutate(Arrays.asList(change));
          if(counter % 1000 ==0){
            System.out.println(counter + " >> wrote key: " + newUuid);
          }
        }
    }
}
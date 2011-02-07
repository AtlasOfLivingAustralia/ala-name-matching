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
;

/**
 * Created by IntelliJ IDEA.
 * User: davejmartin2
 * Date: 07/02/2011
 * Time: 19:02
 * To change this template use File | Settings | File Templates.
 */

object FastDwCLoader {

    def main(args:Array[String]){
        
        if(args.length==2){
            init(args(0))     //   /Users/davejmartin2/dev/biocache-store/conf
            loadWithNativeAPIs(args(1))       //"/data/biocache/ozcam/"
            shutdown
            println("Loading finished. Kill -9 this process")
            exit(1)
        }
        
        if(args.length==1){
            println("loading the UUID mapping")
            retrofitUUIDMapping
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


    def retrofitUUIDMapping {

        var counter = 0
        
        OccurrenceDAO.pageOverAll(Versions.RAW, record => {
            val uniqueID = record.get.occurrence.institutionCode+"|"+record.get.occurrence.collectionCode +"|"+record.get.occurrence.catalogNumber
            DAO.persistentManager.put(uniqueID, "dr", "uuid", record.get.occurrence.uuid)
            counter+=1
            if(counter % 1000 == 0){
              System.out.println(counter + " >> update mapping for: " + record.get.occurrence.uuid+", uniqueID: "+uniqueID);
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
              val property = dwc.getProperty(term)
              if (property != null && property.trim.length > 0)
            } yield (term.simpleName -> property))
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
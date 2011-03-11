package au.org.ala.util

import au.org.ala.biocache.AttributionDAO
import au.org.ala.biocache.Attribution
import java.io.File
import org.wyki.cassandra.pelops.Pelops
/**
 * Loads an export from the old portal database of point lookups.
 * 
 * This should be replaced with access to a webservice.
 *
 * At the moment the webservice does not return the data provider or data resource information
 * 
 * select ic.code, cc.code, icm.institution_uid, icm.collection_uid, ic.name, dp.uid, dp.name, dr.uid, dr.name from inst_coll_mapping icm
  inner join institution_code ic ON ic.id = icm.institution_code_id
  inner join collection_code cc ON cc.id = icm.collection_code_id
  inner join occurrence_record oc on icm.institution_code_id = oc.institution_code_id and icm.collection_code_id = oc.collection_code_id
  inner join data_resource dr on oc.data_resource_id = dr.id
  inner join data_provider dp on oc.data_provider_id = dp.id
  where dr.release_flag
  group by ic.code, cc.code, icm.institution_uid, icm.collection_uid, ic.name,dp.uid, dp.name, dr.uid, dr.name
  into outfile '/tmp/coll-mapping.txt';
 * 
 * @author Dave Martin (David.Martin@csiro.au)
 */
object AttributionLoader {

  def main(args: Array[String]): Unit = {
    import FileHelper._
    println("Starting Collection Loader....")
    val file = new File("/data/biocache/collinst-mapping.txt")
    var counter = 0
    file.foreachLine { line => {
        counter+=1
        //add point with details to
        val parts = line.split("\t")
        var attribution = new Attribution
        attribution.institutionUid = parts(4)
        attribution.collectionUid = parts(2)
        attribution.collectionName = parts(3)
        attribution.institutionName = parts(5)
        attribution.dataProviderUid =parts(6)
        attribution.dataProviderName = parts(7)
        //TODO a collection may belong to mulitple data hubs need to make this multi valued...
        attribution.dataHubUid = parts(8)
        attribution.dataHubName = parts(9)
        if(parts.length>10){
          attribution.taxonomicHints = parts(10).split(";")
          //store them sorted
          scala.util.Sorting.quickSort(attribution.taxonomicHints)
        }
        //There are some clashes where a collection can belong to 2 data resources
//        attribution.dataProviderUid = parts(5)
//        attribution.dataProviderName = parts(6)
//        attribution.dataResourceUid = parts(7)
//        attribution.dataResourceName = parts(8)
        AttributionDAO.add(parts(1), parts(0), attribution)
        if (counter % 1000 == 0) println(counter +": "+parts(1) +"|"+ parts(0))
      }
    }
    println(counter)
    Pelops.shutdown
  }
}
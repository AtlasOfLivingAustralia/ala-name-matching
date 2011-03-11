package au.org.ala.util

import au.org.ala.biocache.TaxonProfileDAO
import au.org.ala.biocache.{TaxonProfile,SensitiveSpecies, States}
import java.net.InetSocketAddress
import org.apache.avro.ipc.SocketTransceiver
import org.apache.avro.specific.SpecificRequestor
import org.apache.avro.util.Utf8
import au.org.ala.bie.rpc.{ ProfileArray, Page, SpeciesProfile }

/**
 * Primes the cache of species profiles. To run this, you need to have
 * bie-rpc server running on top of an existing BIE install.
 * 
 * See bie-rpc for more details.
 */
object TaxonProfileLoader {

  def main(args: Array[String]){

    println("Starting the load...")
    import scala.collection.JavaConversions._

    val host = args(0)
    val port = args(1).toInt
    val client = new SocketTransceiver(new InetSocketAddress(host, port))
    val proxy = SpecificRequestor.getClient(classOf[SpeciesProfile], client).asInstanceOf[SpeciesProfile]
    var page = new Page
    var lastKey = new Utf8("")
    page.startKey = lastKey
    page.pageSize = 1000
    var array = proxy.send(page).asInstanceOf[ProfileArray]

    //iterate through all the profiles
    while(array.profiles.size>0){
	    for (profile <- array.profiles) {
	      //add to cache
	      println(profile)
	      var taxonProfile = new TaxonProfile
	      taxonProfile.guid = profile.guid.toString
	      taxonProfile.scientificName  = profile.scientificName.toString
	      if(profile.commonName!=null){
	    	  taxonProfile.commonName = profile.commonName.toString
	      }
	      if(profile.habitat!=null){
	    	  val habitats = for(habitat<-profile.habitat) yield habitat.toString
	    	  taxonProfile.habitats = habitats.toArray
	      }
              if(profile.left != null){
                taxonProfile.left = profile.left.toString
              }
              if(profile.right != null){
                taxonProfile.right = profile.right.toString
              }
              if(profile.rank != null){
                taxonProfile.rankString = profile.rank.toString
              }
              //TODO work out whatto store from the conservation status

              //store the sensitive species information
              //TODO fix up this so that the zone is obtained from the vocabulary?
              if(profile.sensitiveStatus != null){
                val sss = for(ss <- profile.sensitiveStatus) 
                  yield new SensitiveSpecies(ss.sensitivityZone.toString, ss.sensitivityCategory.toString)              
                  
                taxonProfile.sensitive = sss.toArray
              }
              
	      TaxonProfileDAO.add(taxonProfile)
	      lastKey = profile.guid
	    }
	    page.startKey = lastKey
	    array = proxy.send(page).asInstanceOf[ProfileArray]
    }
    client.close
  }
  println("Finished species profile loading.")
}
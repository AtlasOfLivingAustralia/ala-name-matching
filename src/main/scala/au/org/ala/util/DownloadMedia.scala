package au.org.ala.util

import au.org.ala.biocache.{MediaStore, Config,Json}
import scala.collection.mutable.ArrayBuffer

/**
 * Utility for downloading the media associated with a resource and caching
 * locally.
 */
object DownloadMedia {

  def main(args:Array[String]){
    var dr: String = ""
    val parser = new OptionParser("Download the associated media for a resource") {
      arg("data-resource-uid", "The resource to page over and download the media for", { v: String => dr = v })
    }

    if (parser.parse(args)) {
      Config.occurrenceDAO.pageOverRawProcessed(recordWithOption => {
        if (!recordWithOption.isEmpty){
          val (raw, processed) = recordWithOption.get
          if(raw.occurrence.associatedMedia != null && raw.occurrence.associatedMedia !=""){
            val urls = raw.occurrence.associatedMedia.split(";").map(url => url.trim)
            val imageUrls = urls.filter(MediaStore.isValidImageURL(_))
            val mediaStorePaths = new ArrayBuffer[String]
            imageUrls.foreach( imageUrl => {
              //download it, store it update processed
              try {
                val path = MediaStore.save(raw.uuid, dr, imageUrl)
                mediaStorePaths ++=  path
              } catch {
                case e:Exception => println("Problem downloading from URL: " + imageUrl)
              }
            })
            //update the processed.occurrence.images
            Config.persistenceManager.put(raw.rowKey, "occ", "images.p", Json.toJSON(mediaStorePaths.toArray))
          }
        }
        true
      }, dr + "|", dr + "|~")
    }
    Config.persistenceManager.shutdown
  }
}
package au.org.ala.util

import au.org.ala.biocache.{MediaStore, Config,Json,FullRecord}
import scala.collection.mutable.ArrayBuffer

/**
 * Utility for downloading the media associated with a resource and caching
 * locally.
 */
object DownloadMedia {

  def main(args:Array[String]){
    var dr: String = ""
    var rowKey: String = ""
    val parser = new OptionParser("Download the associated media for a resource") {
      opt("dr","data-resource-uid", "The resource to page over and download the media for", { v: String => dr = v })
      opt("rowkey","row-key-record", "The rowkey for record", { v: String => rowKey = v })
    }

    if (parser.parse(args)) {
      if (dr != "") processDataResource(dr)
      else if (rowKey != "") processRecord(rowKey)
      else parser.showUsage
    }

    Config.persistenceManager.shutdown
  }

  def processUrls(raw:FullRecord, processed:FullRecord, urls: Array[String]) {
    val imageUrls = urls.filter(MediaStore.isValidImageURL(_))
    val mediaStorePaths = new ArrayBuffer[String]
    imageUrls.foreach(imageUrl => {
      //download it, store it update processed
      try {
        val path = MediaStore.save(raw.uuid, raw.attribution.dataResourceUid, imageUrl)
        mediaStorePaths ++= path
      } catch {
        case e: Exception => println("Problem downloading from URL: " + imageUrl)
      }
    })
    //update the processed.occurrence.images
    Config.persistenceManager.put(raw.rowKey, "occ", "associatedMedia", mediaStorePaths.toArray.mkString(";"))
    Config.persistenceManager.put(raw.rowKey, "occ", "images.p", Json.toJSON(mediaStorePaths.toArray))
  }

  /**
   * Process this single record.
   */
  def processRecord(rowKey:String){
    Config.occurrenceDAO.getRawProcessedByRowKey(rowKey) match {
        case Some(rp) => {
           val  (raw, processed) = (rp(0), rp(1))
           if(raw.occurrence.associatedMedia != null && raw.occurrence.associatedMedia !=""){
             val urls = raw.occurrence.associatedMedia.split(";").map(url => url.trim)
             processUrls(raw, processed, urls)
          }
        }
        case None => println("Unrecognised rowkey..." + rowKey)
      }
    }

  /**
   * Download the media for this resource
   */
  def processDataResource(dr:String){
      Config.occurrenceDAO.pageOverRawProcessed(recordWithOption => {
        if (!recordWithOption.isEmpty){
          val (raw, processed) = recordWithOption.get
          if(raw.occurrence.associatedMedia != null && raw.occurrence.associatedMedia !=""){
            val urls = raw.occurrence.associatedMedia.split(";").map(url => url.trim)
            processUrls(raw, processed, urls)
          }
        }
        true
      }, dr + "|", dr + "|~")
    }
}
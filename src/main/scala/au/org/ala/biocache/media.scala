package au.org.ala.biocache

import org.slf4j.LoggerFactory
import org.apache.commons.io.{FilenameUtils, FileUtils}
import java.io.{FilenameFilter, InputStream, File, FileOutputStream}

/**
 * A file store for media files.
 *
 * @author Dave Martin
 */
object MediaStore {

  val rootDir = "/data/biocache-media"
  val limit = 32000
  val logger = LoggerFactory.getLogger("MediaStore")

  def convertPathsToUrls(fullRecord: FullRecord, baseUrlPath: String) {
    if (fullRecord.occurrence.images != null) {
      fullRecord.occurrence.images = fullRecord.occurrence.images.map(x => convertPathToUrl(x,baseUrlPath))
    }
  }

  def convertPathToUrl(str:String,baseUrlPath:String) = str.replaceAll(rootDir, baseUrlPath)

  def exists(uuid: String, resourceUID: String, urlToMedia: String): (String, Boolean) = {
    val path = createFilePath(uuid, resourceUID, urlToMedia)
    (path, (new File(path)).exists)
  }

  def createFilePath(uuid: String, resourceUID: String, urlToMedia: String): String = {
    val subdirectory = (uuid.hashCode % limit).abs

    val absoluteDirectoryPath = rootDir +
      File.separator + resourceUID +
      File.separator + subdirectory +
      File.separator + uuid

    val directory = new File(absoluteDirectoryPath)
    if (!directory.exists) FileUtils.forceMkdir(directory)

    val fileName = {
      if (urlToMedia.lastIndexOf("/") == urlToMedia.length - 1) {
        "raw"
      } else {
        urlToMedia.substring(urlToMedia.lastIndexOf("/") + 1)
      }
    }
    directory.getAbsolutePath + File.separator + fileName
  }

  /**
   * Returns the file path
   */
  def save(uuid: String, resourceUID: String, urlToMedia: String): Option[String] = {
      //handle the situation where the urlToMedia does not exits - 
    try{  
        val fullPath = createFilePath(uuid, resourceUID, urlToMedia)
        val file = new File(fullPath)
        val in = (new java.net.URL(urlToMedia)).openStream
        val out = new FileOutputStream(file)
        val buffer: Array[Byte] = new Array[Byte](1024)
        var numRead = 0
        while ( {
          numRead = in.read(buffer); numRead != -1
        }) {
          out.write(buffer, 0, numRead)
          out.flush
        }
        out.close
        logger.info("File saved to: " + fullPath)
        //store the media
        Some(fullPath)
    }
    catch{
        case e:Exception => logger.warn("Unable to load media " + urlToMedia + ". " + e.getMessage);None
    }
  }

  def alternativeFormats(filePath:String) : Array[String] = {
    val file = new File(filePath)
    file.exists match {
      case true => {
        val filenames = file.getParentFile.list(new SameNameDifferentExtensionFilter(filePath))
        filenames.map(f => file.getParent + File.separator + f)
      }
      case false => Array()
    }
  }
}

class SameNameDifferentExtensionFilter(name:String) extends FilenameFilter {
  val nameToMatch = FilenameUtils.removeExtension(FilenameUtils.getName(name)).toLowerCase
  def accept(dir: File, name: String) = FilenameUtils.removeExtension(name.toLowerCase) == nameToMatch
}
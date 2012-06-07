package au.org.ala.biocache

import org.slf4j.LoggerFactory
import org.apache.commons.io.{FilenameUtils, FileUtils}
import java.io._
import javax.imageio.ImageIO
import javax.media.jai.{RenderedOp, JAI}
import com.sun.media.jai.codec.FileSeekableStream
import java.awt.{Graphics2D, Image}
import java.awt.image.BufferedImage
import au.org.ala.util.OptionParser
import au.org.ala.util.OptionParser

/**
 * A file store for media files.
 *
 * @author Dave Martin
 */
object MediaStore {

  val logger = LoggerFactory.getLogger("MediaStore")

  //Regular expression used to parse an image URL - adapted from
  //http://stackoverflow.com/questions/169625/regex-to-check-if-valid-url-that-ends-in-jpg-png-or-gif#169656
  lazy val imageParser = """^(https?://(?:[a-zA-Z0-9\-]+\.)+[a-zA-Z]{2,6}(?:/[^/#]+)+\.(?:jpg|gif|png|jpeg))$""".r
  lazy val soundParser = """^(https?://(?:[a-zA-Z0-9\-]+\.)+[a-zA-Z]{2,6}(?:/[^/#]+)+\.(?:wav|mp3|ogg|flac))$""".r
  lazy val videoParser = """^(https?://(?:[a-zA-Z0-9\-]+\.)+[a-zA-Z]{2,6}(?:/[^/#]+)+\.(?:wmv|mp4|mpg|avi|mov))$""".r

  val imageExtension = Array(".jpg",".gif",".png",".jpeg")
  val soundExtension = Array(".wav",".mp3",".ogg",".flac")
  val videoExtension = Array(".wmv",".mp4",".mpg",".avi",".mov")

  val rootDir = "/data/biocache-media"
  val limit = 32000

  def doesFileExist(urlString:String) :Boolean ={
    val urlToTest = if(urlString.startsWith(rootDir)) "file://"+urlString else urlString
      try{
        val url = new java.net.URL(urlToTest.replaceAll(" ", "%20"))
        val in = url.openStream
        true
      }
      catch{
        case _ => false
      }
  }

  def isValidImageURL(url:String) : Boolean = {
    !imageParser.unapplySeq(url.trim.toLowerCase).isEmpty || isStoredMedia(imageExtension,url)
  }

  def isValidSoundURL(url:String) : Boolean = {
    !soundParser.unapplySeq(url.trim.toLowerCase).isEmpty || isStoredMedia(soundExtension,url)
  }

  def isValidVideoURL(url:String) : Boolean = {
    !videoParser.unapplySeq(url.trim.toLowerCase).isEmpty || isStoredMedia(videoExtension,url)
  }

  def isStoredMedia(acceptedExtensions:Array[String], url:String) : Boolean = {
     url.startsWith(MediaStore.rootDir) && endsWithOneOf(acceptedExtensions,url.toLowerCase)
  }

  def endsWithOneOf(acceptedExtensions:Array[String], url:String) : Boolean = {
    !(acceptedExtensions collectFirst { case x if url.endsWith(x) => x } isEmpty)
  }

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

  /**
   * Create a file path for this UUID and resourceUID
   */
  def createFilePath(uuid: String, resourceUID: String, urlToMedia: String): String = {
    val subdirectory = (uuid.hashCode % limit).abs

    val absoluteDirectoryPath = rootDir +
      File.separator + resourceUID +
      File.separator + subdirectory +
      File.separator + uuid

    val directory = new File(absoluteDirectoryPath)
    if (!directory.exists) FileUtils.forceMkdir(directory)

    val fileName = {
      if(urlToMedia.contains("fileName=")){  //HACK for CS URLs which dont make for nice file names
        urlToMedia.substring(urlToMedia.indexOf("fileName=") + "fileName=".length).replace(" ", "_")
      } else if (urlToMedia.lastIndexOf("/") == urlToMedia.length - 1) {
        "raw"
      } else {
        urlToMedia.substring(urlToMedia.lastIndexOf("/") + 1).replace(" ", "_")
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
        val url = new java.net.URL(urlToMedia.replaceAll(" ", "%20"))
        val in = url.openStream
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
        //is this file an image???
        if (isValidImageURL(urlToMedia)){
           Thumbnailer.generateAllSizes(new File(fullPath))
        }
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

trait ImageSize {
  def suffix:String
  def size:Float
}

object THUMB extends ImageSize { def suffix = "__thumb"; def size = 100f; }
object SMALL extends ImageSize { def suffix = "__small"; def size = 314f; }
object LARGE extends ImageSize { def suffix = "__large"; def size = 650f; }

/**
 * A utility for thumbnailing images
 */
object Thumbnailer {
  final val logger = LoggerFactory.getLogger("Thumbnailer")
  System.setProperty("com.sun.media.jai.disableMediaLib", "true")

  /**
   * Runnable for generating thumbnails
   */
  def main(args:Array[String]){
    var directoryPath = ""
    var filePath = ""

    val parser = new OptionParser("Thumbnail generator") {
      opt("f", "absolute-file-path", "File path to image to generate thumbnails for", { v: String => filePath = v })
      opt("d", "absolute-directory-path", "Directory path to recursively", { v: String => directoryPath = v })
    }
    if (parser.parse(args)) {
      if (filePath !=""){
        generateAllSizes(new File(filePath))
      }
      if (directoryPath !=""){
        recursivelyGenerateThumbnails(new File(directoryPath))
      }
    }
  }

  /**
   * Recursively crawl directories and generate thumbnails
   */
  def recursivelyGenerateThumbnails(directory:File){
    //dont generate thumbnails for thumbnails
    println("Starting with directory: " + directory.getAbsolutePath)
    if (directory.isDirectory) {
        var children = directory.list
        if (children == null) {
            children = Array[String]()
        }
        println("Recursive Dir: " + directory.getName + ", size of subDirs: " + children.length);
        for (i <- 0 until children.length) {
            recursivelyGenerateThumbnails(new File(directory, children(i)))
        }
    } else {
      //generate a thumbnail if this is an image
      if (MediaStore.isValidImageURL(directory.getAbsolutePath)){
        generateAllSizes(directory)
      }
    }
  }

  /**
   * Generate thumbnails of all sizes
   */
  def generateAllSizes(source:File){
    val fileName = source.getName
    if(!fileName.contains(THUMB.suffix) && !fileName.contains(SMALL.suffix) && !fileName.contains(LARGE.suffix)){
      generateThumbnail(source, THUMB)
      generateThumbnail(source, SMALL)
      generateThumbnail(source, LARGE)
    }
  }

  /**
   * Generate an image of the specified size.
   */
  def generateThumbnail(source:File, imageSize:ImageSize){
    val extension = FilenameUtils.getExtension(source.getAbsolutePath)
    val targetFilePath = source.getAbsolutePath.replace("." + extension, imageSize.suffix + "." + extension)
    val target = new File(targetFilePath)
    generateThumbnail(source, target, imageSize.size)
  }

  /**
   * Generatea thumbanail to the specified file.
   */
  def generateThumbnail(source:File, target:File, thumbnailSize:Float){
    val t = new ThumbnailableImage(source)
    t.writeThumbnailToFile(target, thumbnailSize)
  }
}

/**
 * An image that can be thumbnailed
 */
class ThumbnailableImage(imageFile:File) {

  final val logger = LoggerFactory.getLogger("ThumbnailableImage")
  final val fss = new FileSeekableStream(imageFile)
  final val originalImage = JAI.create("stream", fss)

  /**
   * Write a thumbnail to file
   */
  def writeThumbnailToFile(newThumbnailFile:File, edgeLength:Float) {

    val height = originalImage.getHeight
    val width = originalImage.getWidth
    val renderedImage = originalImage.createSnapshot.asInstanceOf[javax.media.jai.RenderedOp]
    if (!(height < edgeLength && width < edgeLength)) {
      val denom = {
        if (height > width) height
        else width
      }
      val modifier = edgeLength / denom
      val w = (width * modifier).toInt
      val h = (height * modifier).toInt
      val i = renderedImage.getAsBufferedImage.getScaledInstance(w, h, Image.SCALE_SMOOTH)
      val bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
      val g = bufferedImage.createGraphics
      g.drawImage(i, null, null)
      g.dispose
      i.flush
      val modifiedImage = JAI.create("awtImage", bufferedImage.asInstanceOf[Image])
      val fOut = new FileOutputStream(newThumbnailFile)
      ImageIO.write(modifiedImage, "jpg", fOut)
      fOut.flush
      fOut.close
    } else {
      FileUtils.copyFile(imageFile, newThumbnailFile)
    }
  }
}
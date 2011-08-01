package au.org.ala.biocache

import java.io.InputStream
import java.io.File
import org.apache.commons.io.FileUtils
import java.io.FileOutputStream
import org.slf4j.LoggerFactory
/**
 * A file store for media files.
 * 
 * @author Dave Martin
 */
object MediaStore {
	
    val rootDir = "/data/biocache-media"
    val limit = 32000
    val logger = LoggerFactory.getLogger("MediaStore")
    
    def exists(uuid:String, resourceUID: String, urlToMedia:String) : (String, Boolean) = {
        val path = createFilePath(uuid,resourceUID,urlToMedia)
        (path, (new File(path)).exists)
    }
    
    def createFilePath(uuid:String, resourceUID: String, urlToMedia:String) : String = {
        val subdirectory = (uuid.hashCode % limit ).abs
        
        val absoluteDirectoryPath = rootDir +
        	File.separator + resourceUID +
        	File.separator + subdirectory + 
        	File.separator + uuid
        
        val directory = new File(absoluteDirectoryPath)
        if(!directory.exists) FileUtils.forceMkdir(directory)
        
        val fileName = {
            if(urlToMedia.lastIndexOf("/") == urlToMedia.length-1){
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
    def save(uuid:String, resourceUID: String, urlToMedia:String) : String = {
        
        val fullPath = createFilePath(uuid,resourceUID,urlToMedia)
        val file = new File(fullPath)
        val in = (new java.net.URL(urlToMedia)).openStream
        val out = new FileOutputStream(file)
        val buffer: Array[Byte] = new Array[Byte](1024)
        var numRead = 0
        while ({ numRead = in.read(buffer); numRead != -1 }) {
            out.write(buffer, 0, numRead)
            out.flush
        }
        out.close
        logger.info("File saved to: " + fullPath)
        //store the media
        fullPath
    }
}
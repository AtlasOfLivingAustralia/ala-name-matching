/**
 * 
 */
package au.org.ala.biocache

import java.io.InputStream
import java.io.File
import org.apache.commons.io.FileUtils
import java.io.FileOutputStream
/**
 * A file store for media files.
 * 
 * @author Dave Martin
 */
object MediaStore {
	
    val rootDir = "/data/biocache-media"
    val limit = 32000
    
    /**
     * Returns the file path
     */
    def save(uuid:String, resourceUID: String, urlToMedia:String) : String = {
        
        val subdirectory = (uuid.hashCode % limit ).abs
        
        val absoluteDirectoryPath = rootDir +
        	File.separator + resourceUID +
        	File.separator + subdirectory + 
        	File.separator + uuid
        
        val directory = new File(absoluteDirectoryPath)
        if(!directory.exists) FileUtils.forceMkdir(directory)
        
        val in = (new java.net.URL(urlToMedia)).openStream
        
        val fileName = {
            if(urlToMedia.lastIndexOf("/") == urlToMedia.length-1){
            	"raw"
            } else {
                urlToMedia.substring(urlToMedia.lastIndexOf("/") + 1)
            }
        }
        
        val fullPath = directory.getAbsolutePath + File.separator + fileName
        val file = new File(fullPath)
        val out = new FileOutputStream(file)
        val buffer: Array[Byte] = new Array[Byte](1024)
        var numRead = 0
        while ({ numRead = in.read(buffer); numRead != -1 }) {
            out.write(buffer, 0, numRead)
            out.flush
        }
        out.close
        
        //store the media
        fullPath
    }
}
package au.org.ala.biocache.util

import java.io.File
import java.util.Date
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Metadata
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.exif.ExifSubIFDDirectory

/**
 * Simple utility for extracting coordinates and a timestamp from an image
 */
object EXIF {

  def main(args : Array[String]) : Unit = {
      val directory = new File("/data/biocache-media/dr360/")
      exifDirectory(directory)
  }
  
  def exifDirectory(directory:File){
      
      directory.listFiles.foreach(file => {
          if(file.isDirectory()){
        	  exifDirectory(file)
          } else {
        	  println(extractCoordinates(file))
          }
      })
  }
  
  /**
   * Extract coordinates from xif
   */
  def extractCoordinates(imageFile:File) : Option[(Double, Double, Date)] = {
      
      val metadata:Metadata = ImageMetadataReader.readMetadata(imageFile)
      val gpsDirectory =  metadata.getDirectory(classOf[GpsDirectory])
      if(gpsDirectory != null){
	      val latitudeRef = gpsDirectory.getString(GpsDirectory.TAG_GPS_LATITUDE_REF)
	      val longitudeRef = gpsDirectory.getString(GpsDirectory.TAG_GPS_LONGITUDE_REF)
	      
	      val latArray = gpsDirectory.getRationalArray(GpsDirectory.TAG_GPS_LATITUDE)
	      val latitude = {
	          val deg = latArray(0).doubleValue.abs
	          val min = latArray(1).doubleValue.abs
	          val sec = latArray(2).doubleValue.abs
	          val ref = if(latitudeRef == "S") -1.0 else 1.0
	          (deg  + ((min * 60 + sec)/3600.0)) * ref
	      }
	      
	      val lonArray = gpsDirectory.getRationalArray(GpsDirectory.TAG_GPS_LONGITUDE)
	      val longitude = {
	          val deg = lonArray(0).doubleValue
	          val min = lonArray(1).doubleValue
	          val sec = lonArray(2).doubleValue
	          val ref = if(latitudeRef == "W") -1.0 else 1.0
	          (deg  + ((min * 60 + sec)/3600.0)) * ref
	      }
	
	      val directory = metadata.getDirectory(classOf[ExifSubIFDDirectory])
	      val date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
	      
	      Some((latitude, longitude, date))
      } else {
          None
      }
  }
}


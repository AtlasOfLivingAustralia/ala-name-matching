package au.org.ala.biocache.outliers

import math._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}


object JackKnife {

  /**
   * Takes a list of sampled values
   */
  def jackknife(sampledUnsorted:List[Float]) : Option[List[Float]] = {

    println("Jacknife testing: " + sampledUnsorted)
    
    if (sampledUnsorted.isEmpty) return None

    val samples = sampledUnsorted.sorted
    val outliers = Array.fill(samples.size)(false)
    val outlierness = Array.fill(samples.size)(0.0)
    val cArray = Array.fill(samples.size)(0.0)
    val yArray = Array.fill(samples.size)(0.0)

    val n = samples.size

    val min = samples.min
    val max = samples.max
    val smean = mean(samples)
    val sstd = stddev(samples)
    val srange = max - min
    val threshold = (0.95 * sqrt(n) + 0.2 ) * (srange/50)

    if (threshold <= 0) return None

    //interate through samples generating
    for(i <- 0 until n){

      val y = samples(i) compare smean match {
          case -1  => (samples(i + 1) - samples(i)) * (smean - samples(i))
          case  1  => (samples(i) - samples(i - 1)) * (samples(i) - smean)
          case  0  => 0.0
      }

      //println("for " + i + ", sample: " + samples(i) + ", y = " + y)

      val c = y / sstd

      if (c > threshold){
        //println("************ outlier!!!!!!!")
        outliers(i) = true

        if (samples(i) < smean && i > 0){
          var q = i
          while(q >= 0){
            outliers(q) = true
            q = q - 1
          }
        } else if (samples(i) > smean && i < n-1) {
          var q = i
          while(q < n){
            outliers(q) = true
            q = q + 1
          }
        }
      }
      cArray(i) = c.toFloat
      yArray(i) = y.toFloat
    }
    
    for(i <- 0 to n-1){
      if (samples(i) > smean && outliers.contains(samples(i))){
        cArray(i) = cArray(i) + cArray(i - 1)
      }
    }
    
    for(i <- 0 until n-1){
      if(samples(i) < smean && outliers.contains(samples(i))){
        cArray(i) = cArray(i) + cArray(i + 1)
      }
    }

    //calculate the outlierness
    for(i <- 0 until n){
    	outlierness(i) = (cArray(i) / threshold).toFloat
    }
    
    println("min : " + min)
    println("max : " + max)
    println("sample size : " + n)
    println("smean : " + smean)
    println("sstd : " + sstd)
    println("srange : " + srange)
    println("threshold : " + threshold)    
    println("outlierness values: " + outlierness.toList)
    println("c values: " + cArray.toList)
    println("y values: " + yArray.toList)
    
    val outlierValues = new ArrayBuffer[Float]
    for (i <- 0 until outliers.length){
    	if(outliers(i))
    	  outlierValues += samples(i)
    }
    
    println("outliers values: " + outlierValues.toList)
    
    println("sample size: " + samples.size +", no of outliers: " + outlierValues.size)
    Some(outlierValues.toList)
  }


  def mean(values:List[Float]) : Float = values.foldLeft(0.0)(_+_).toFloat / values.size.toFloat

  def stddev(values:List[Float]) : Float = {
    val smean = mean(values)
    sqrt( values.foldLeft(0.0)( (total,value) => total + pow((value - smean),2.0) ) / values.size.toFloat).toFloat
  }
}
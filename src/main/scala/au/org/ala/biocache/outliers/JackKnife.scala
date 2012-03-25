package au.org.ala.biocache.outliers

import math._
import scala.collection.mutable.{ArrayBuffer}
import reflect.BeanProperty

/**
 * Code for running jacknife against a set of float values.
 */
class JackKnife {

  /** Takes a list of sampled values */
  def jackknife(sampledUnsorted:Seq[Float]) : Option[JackKnifeStats]  = {

    if (sampledUnsorted.isEmpty) return None

    val samples = sampledUnsorted.sorted    //sorted floats
    val outliers = Array.fill(samples.size)(false)
    val outlierness = Array.fill(samples.size)(0.0f)
    val cArray:Array[Float] = Array.fill(samples.size)(0.0f)
    val yArray:Array[Float] = Array.fill(samples.size)(0.0f)

    val n = samples.size

    val min = samples.min
    val max = samples.max
    val smean = mean(samples)
    val sstd = stddev(samples)
    val srange = max - min
    val threshold = ((0.95 * sqrt(n) + 0.2 ) * (srange/50)).toFloat

    if (threshold <= 0) return None

    //interate through samples generating
    for(i <- 0 until n){

      val y = samples(i) compare smean match {
        case -1 => (samples(i + 1) - samples(i)) * (smean - samples(i))
        case  1 => (samples(i) - samples(i - 1)) * (samples(i) - smean)
        case  0 => 0.0
      }

      val c = y / sstd

      if (c > threshold){
        outliers(i) = true

        if (samples(i) < smean && i > 0){
          var q = i
          while(q >= 0){
            outliers(q) = true
            q = q - 1
          }
        }
        if (samples(i) > smean && i < n-1) {
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
      if (samples(i) > smean && outliers(i-1)){
        cArray(i) = cArray(i) + cArray(i - 1)
      }
    }
    
    var i = n - 1
    while(i >= 0){
      if(samples(i) < smean && outliers(i)){
        cArray(i) = cArray(i) + cArray(i + 1)
      }
      i = i - 1
    }

    //calculate the outlierness
    for(i <- 0 until n){
    	outlierness(i) = (cArray(i) / threshold).toFloat
    }

   /*
    for (i <- 0 until outliers.size){
      println("Sample: " + samples(i) + ", outlier: "+ outliers(i))
    }
    */
    
    val outlierValues = new ArrayBuffer[Float]
    for (i <- 0 until outliers.length){
    	if(outliers(i)) outlierValues += samples(i)
    }
    
    
    val triggerFailsafe:Boolean = (outlierValues.size > (samples.length / 2))
    if(triggerFailsafe)
      outlierValues.clear()
    
   // println("outliers values: " + outlierValues.toList)
    //println("sample size: " + samples.size +", no of outliers: " + outlierValues.size)
    Some(JackKnifeStats(n,min,max,smean,sstd,srange,threshold,outlierValues.toArray,triggerFailsafe))
  }

  def mean(values:Seq[Float]) : Float = values.foldLeft(0.0)(_+_).toFloat / values.size.toFloat

  def stddev(values:Seq[Float]) : Float = {
    val smean = mean(values)
    sqrt( values.foldLeft(0.0)( (total,value) => total + pow((value - smean),2.0) ) / values.size.toFloat).toFloat
  }
}

/** Encapsulates the results of running Jacknife */
case class JackKnifeStats(@BeanProperty sampleSize:Int,
                           @BeanProperty min:Float,
                           @BeanProperty max:Float,
                           @BeanProperty mean:Float,
                           @BeanProperty stdDev:Float,
                           @BeanProperty range:Float,
                           @BeanProperty threshold:Float,
                           @BeanProperty outlierValues:Array[Float],
                           @BeanProperty triggerFailsafe:Boolean)

case class RecordJackKnifeStats(@BeanProperty uuid:String,
                          @BeanProperty layerId:String,
                          @BeanProperty recordLayerValue:Float,
                          @BeanProperty sampleSize:Int,
                          @BeanProperty min:Float,
                          @BeanProperty max:Float,
                          @BeanProperty mean:Float,
                          @BeanProperty stdDev:Float,
                          @BeanProperty range:Float,
                          @BeanProperty threshold:Float,
                          @BeanProperty outlierValues:Array[Float])

case class SampledRecord(id: String, value: Float, cellId: Int)
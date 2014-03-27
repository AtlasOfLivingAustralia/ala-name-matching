package au.org.ala.biocache.model

import scala.beans.BeanProperty

/**
 * POSO for an outlier result
 */
class OutlierResult (@BeanProperty var testUuid:String, @BeanProperty var outlierForLayersCount:Int)

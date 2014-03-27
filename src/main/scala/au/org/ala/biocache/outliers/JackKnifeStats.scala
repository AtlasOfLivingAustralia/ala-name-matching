package au.org.ala.biocache.outliers

import scala.beans.BeanProperty

/**
 * Encapsulates the results of running Jacknife
 */
case class JackKnifeStats(@BeanProperty sampleSize: Int,
                          @BeanProperty min: Float,
                          @BeanProperty max: Float,
                          @BeanProperty mean: Float,
                          @BeanProperty stdDev: Float,
                          @BeanProperty range: Float,
                          @BeanProperty threshold: Float,
                          @BeanProperty outlierValues: Array[Float],
                          @BeanProperty triggerFailsafe: Boolean)

package au.org.ala.biocache.outliers

import scala.beans.BeanProperty

case class RecordJackKnifeStats(@BeanProperty uuid: String,
                                @BeanProperty layerId: String,
                                @BeanProperty recordLayerValue: Float,
                                @BeanProperty sampleSize: Int,
                                @BeanProperty min: Float,
                                @BeanProperty max: Float,
                                @BeanProperty mean: Float,
                                @BeanProperty stdDev: Float,
                                @BeanProperty range: Float,
                                @BeanProperty threshold: Float,
                                @BeanProperty outlierValues: Array[Float])
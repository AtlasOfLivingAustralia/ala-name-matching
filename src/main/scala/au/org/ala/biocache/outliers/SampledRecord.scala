package au.org.ala.biocache.outliers

import scala.beans.BeanProperty

case class SampledRecord(@BeanProperty id: String,
                         @BeanProperty value: Float,
                         @BeanProperty cellId: Int,
                         @BeanProperty rowKey: Option[String] = None)
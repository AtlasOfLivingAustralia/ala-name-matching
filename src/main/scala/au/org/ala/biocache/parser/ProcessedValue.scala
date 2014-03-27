package au.org.ala.biocache.parser

import scala.beans.BeanProperty

case class ProcessedValue(@BeanProperty name: String, @BeanProperty raw: String, @BeanProperty processed: String)
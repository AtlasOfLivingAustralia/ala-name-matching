package au.org.ala.biocache.parser

import scala.beans.BeanProperty
import au.org.ala.biocache.model.QualityAssertion

case class ParsedRecord(@BeanProperty values: Array[ProcessedValue], @BeanProperty assertions: Array[QualityAssertion])
package au.org.ala.biocache.caches

import scala.io.Source

object WebServiceLoader {

  def getWSStringContent(url: String) = try {
    Source.fromURL( url ).mkString
  } catch {
    case e: Exception => ""
  }
}
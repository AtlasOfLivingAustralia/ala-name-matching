package au.org.ala.biocache.vocab

object DwC extends Vocab {

  val junk = List("matched", "parsed", "processed", "-", "\\.","_")

  override def matchTerm(string2Match: String) = {
    val str = {
      var strx = string2Match.toLowerCase
      junk.foreach( j => { strx = strx.replaceAll(j,"") })
      strx.trim
    }
    super.matchTerm(str)
  }

  val all = loadVocabFromFile("/dwc.txt")
}

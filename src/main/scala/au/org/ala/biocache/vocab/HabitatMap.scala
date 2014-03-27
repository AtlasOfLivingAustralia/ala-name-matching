package au.org.ala.biocache.vocab

/**
 * A vocabulary mapping for habitats.
 */
object HabitatMap extends VocabMaps {
  //Assume that all habitats fit into a "MARINE AND NON-MARINE" environment.
  val termMap = Map(
    "MARINE" -> Array("MARINE","MARINE AND NON-MARINE"),
    "NON-MARINE" -> Array("NON-MARINE", "TERRESTRIAL", "LIMNETIC","MARINE AND NON-MARINE"),
    "TERRESTRIAL" -> Array("NON-MARINE", "TERRESTRIAL", "LIMNETIC","MARINE AND NON-MARINE"),
    "LIMNETIC" -> Array("NON-MARINE", "TERRESTRIAL", "LIMNETIC","MARINE AND NON-MARINE")
  )
}

package au.org.ala.biocache.vocab

/**
 * Case class that stores the information required to map a species to its
 * associated groups
 */
case class SpeciesGroup(name:String, rank:String, values:Array[String], excludedValues:Array[String], lftRgtValues:Array[(Int,Int, Boolean)], parent:String){

  /*
   * Determines whether the supplied lft value represents a species from this group/
   * Relies on the excluded values coming first in the lftRgtValues array
   */
  def isPartOfGroup(lft:Int):Boolean ={
    lftRgtValues.foreach { tuple =>
      val (l,r,include) = tuple
    if(lft >= l && lft < r)
      return include
    }
    false
  }
  override def toString :String = {
    "SpeciesGroup(name:"+name +", rank:"+rank+", values: "+values.mkString("[",",","]") +", excludedValues: " + excludedValues.mkString("[",",","]") + ", lftRgtValues: "+lftRgtValues.mkString("[",",","]")
  }
}

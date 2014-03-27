package au.org.ala.biocache.vocab

import beans.BeanProperty
import au.org.ala.biocache.util.Stemmer

/** Case class that encapsulates a canonical form and variants. */
class Term (@BeanProperty val canonical:String, @BeanProperty rawVariants:Array[String]){
  val variants = rawVariants.map(v => v.toLowerCase.trim) ++ rawVariants.map(v => Stemmer.stem(v)) :+ Stemmer.stem(canonical)
}

/** Factory for terms */
object Term {
  def apply(canonical: String): Term = new Term(canonical, Array[String]())
  def apply(canonical: String, variant: String): Term = new Term(canonical, Array(variant))
  def apply(canonical: String, variants: String*): Term = new Term(canonical, Array(variants:_*))
  def apply(canonical: String, variants: Array[String]): Term = new Term(canonical, variants)
}
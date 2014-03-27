package au.org.ala.biocache.model

import scala.beans.BeanProperty
import au.org.ala.biocache.poso.POSO

/**
 * POSO for handling identification information for an occurrence.
 */
class Identification extends Cloneable with POSO {
  override def clone : Identification = super.clone.asInstanceOf[Identification]
  @BeanProperty var dateIdentified:String = _
  @BeanProperty var identificationAttributes:String = _
  @BeanProperty var identificationID:String = _
  @BeanProperty var identificationQualifier:String = _
  @BeanProperty var identificationReferences:String = _
  @BeanProperty var identificationRemarks:String = _
  @BeanProperty var identificationVerificationStatus:String = _
  @BeanProperty var identifiedBy:String = _
  @BeanProperty var identifierRole:String = _ //HISPID addition http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0376
  @BeanProperty var typeStatus:String = _
  /* AVH addition */
  @BeanProperty var abcdTypeStatus:String = _ //ABCD addition for AVH http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0645
  @BeanProperty var typeStatusQualifier:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0647
  @BeanProperty var typifiedName:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0604
  @BeanProperty var verbatimDateIdentified:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0383
  @BeanProperty var verifier:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0649
  @BeanProperty var verificationDate:String =_ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0657
  @BeanProperty var verificationNotes:String = _ //http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0658
  @BeanProperty var abcdIdentificationQualifier:String =_ //ABCD addition for AVH http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0332
  @BeanProperty var abcdIdentificationQualifierInsertionPoint:String =_ //ABCD addition for AVH http://wiki.tdwg.org/twiki/bin/view/ABCD/AbcdConcept0333
}

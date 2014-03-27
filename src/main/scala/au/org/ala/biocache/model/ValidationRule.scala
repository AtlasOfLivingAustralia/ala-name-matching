package au.org.ala.biocache.model

import org.codehaus.jackson.annotate.JsonIgnoreProperties
import scala.beans.BeanProperty
import au.org.ala.biocache.poso.POSO

/**
 * A type of validation rule for occurrences.
 */
@JsonIgnoreProperties(Array("id", "rawAssertion", "rawQuery", "records"))
class ValidationRule (
  @BeanProperty var rowKey:String = null,
  @BeanProperty var id:String = null,
  @BeanProperty var uuid:String = null,
  @BeanProperty var apiKey:String = null,
  @BeanProperty var rawAssertion:String = null,
  @BeanProperty var createdDate:java.util.Date = null,
  @BeanProperty var modifiedDate:java.util.Date = null,
  @BeanProperty var rawQuery:String = null,
  @BeanProperty var qidQuery:String = null,
  @BeanProperty var deletedDate:java.util.Date = null,
  @BeanProperty var userId:String = null,
  @BeanProperty var userEmail:String = null,
  @BeanProperty var userName:String = null,
  @BeanProperty var authority:String = null,
  @BeanProperty var assertionType:String = null,
  @BeanProperty var comment: String = null,
  @BeanProperty var wkt: String = null,
  @BeanProperty var includeNew:Boolean = true,
  @BeanProperty var disabled:Boolean = false,
  @BeanProperty var lastApplied:java.util.Date = null,
  @BeanProperty var records:Array[String] = Array()
  ) extends POSO {

  var recordCount = 0

  def this() = this(records = Array())

  //The number of records that have been applied to this assertion
  def getRecordCount : Int = if(recordCount == 0) records.size else recordCount
  def setRecordCount(value:Int) = recordCount = value
}

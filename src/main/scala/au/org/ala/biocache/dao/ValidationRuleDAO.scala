package au.org.ala.biocache.dao

import au.org.ala.biocache.model.ValidationRule

/**
 * DAO for validation rules
 */
trait ValidationRuleDAO extends DAO {

  val entityName = "queryassert"

  def get(id:String) : Option[ValidationRule]

  def list : List[ValidationRule]

  def get(ids:List[String]) : List[ValidationRule]

  def upsert(validationRule:ValidationRule)

  def delete(id:String, date:java.util.Date=null, physicallyRemove:Boolean=false)

  def pageOver(proc: (Option[ValidationRule] => Boolean),startKey:String="", endKey:String="", pageSize: Int = 1000): Unit

  def createOrRetrieveUuid(uniqueID: String): String
}

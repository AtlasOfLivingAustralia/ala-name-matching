package au.org.ala.biocache.model

import java.util
import scala.beans.BeanProperty

class DuplicateRecordDetails(@BeanProperty var rowKey:String, @BeanProperty var uuid:String, @BeanProperty var taxonConceptLsid:String,
                             @BeanProperty var year:String, @BeanProperty var month:String, @BeanProperty var day:String,
                             @BeanProperty var point1:String, @BeanProperty var point0_1:String,
                             @BeanProperty var point0_01:String, @BeanProperty var point0_001:String,
                             @BeanProperty var point0_0001:String,@BeanProperty var latLong:String,
                             @BeanProperty var rawScientificName:String, @BeanProperty var collector:String,
                             @BeanProperty var oldStatus:String, @BeanProperty var oldDuplicateOf:String){

  def this() = this(null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null)

  @BeanProperty var status = "U"
  @BeanProperty var druid:String = {
    if(rowKey != null){
      rowKey.split("\\|")(0)
    } else {
      null
    }
  }
  var duplicateOf:String = null
  //stores the precision so that coordinate dup types can be established - we don't want to persist this property
  var precision = 0
  @BeanProperty var duplicates:util.ArrayList[DuplicateRecordDetails]=null
  @BeanProperty var dupTypes:util.ArrayList[DupType]=_

  def addDuplicate(dup:DuplicateRecordDetails){
    if(duplicates == null){
      duplicates = new util.ArrayList[DuplicateRecordDetails]
    }
    duplicates.add(dup)
  }

  def addDupType(dup:DupType){
    if(dupTypes == null){
      dupTypes = new util.ArrayList[DupType]()
    }
    dupTypes.add(dup)
  }
}
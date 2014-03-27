package au.org.ala.biocache.vocab

import scala.beans.BeanProperty

/**
 * Case class that represents an error code for a occurrence record.
 */
sealed case class ErrorCode(@BeanProperty name:String,
  @BeanProperty code:Int,
  @BeanProperty isFatal:Boolean,
  @BeanProperty description:String,
  @BeanProperty category:String = ErrorCodeCategory.Error
)

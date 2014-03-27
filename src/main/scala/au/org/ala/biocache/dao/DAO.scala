package au.org.ala.biocache.dao

import java.util.UUID

/**
 * Trait for DAOs to extend
 */
trait DAO {

  /**
   * Create an uuid
   * @return an uuid string
   */
  def createUuid = UUID.randomUUID.toString
}

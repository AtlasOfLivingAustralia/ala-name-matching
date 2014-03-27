package au.org.ala.biocache.model

/**
 * POSO representing a measurement unit
 */
abstract sealed class MeasurementUnit

case object Metres extends MeasurementUnit
case object Kilometres extends MeasurementUnit
case object Feet extends MeasurementUnit

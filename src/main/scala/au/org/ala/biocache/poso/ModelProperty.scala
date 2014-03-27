package au.org.ala.biocache.poso

import java.lang.reflect.Method

/**
 * Holds the details of a property for a bean
 */
case class ModelProperty(name: String, typeName: String, getter: Method, setter: Method)

package au.org.ala.biocache.poso

import scala.collection.mutable
import java.lang.reflect.Method
import org.apache.commons.lang3.StringUtils

/**
  * A singleton that keeps a cache of POSO reflection metadata.
  */
object ReflectionCache {

   var posoLookupCache = new mutable.HashMap[Class[_], Map[String, ModelProperty]]
   var compositeLookupCache = new mutable.HashMap[Class[_], Map[String, Method]]

   def getCompositeLookup(cposo: CompositePOSO): Map[String, Method] = {

     val result = compositeLookupCache.get(cposo.getClass)

     if (result.isEmpty) {
       val map = new mutable.HashMap[String, Method]()
       cposo.getClass.getDeclaredFields.map(field => {
         val name = field.getName
         try {
           val getter = cposo.getClass.getDeclaredMethod("get" + StringUtils.capitalize(name))
           val isAPoso = !(getter.getReturnType.getInterfaces.forall(i => i == classOf[POSO]))
           if (isAPoso) {
             val poso = getter.invoke(cposo).asInstanceOf[POSO]
             poso.propertyNames.foreach(name => map += (name -> getter))
           }
         } catch {
           case e: Exception =>
         }
       })
       val fieldMap = map.toMap
       compositeLookupCache.put(cposo.getClass, fieldMap)
       fieldMap
     } else {
       result.get
     }
   }

   def getPosoLookup(poso: POSO): Map[String, ModelProperty] = {

     val result = posoLookupCache.get(poso.getClass)

     if (result.isEmpty) {
       val posoLookupMap = poso.getClass.getDeclaredFields.map(field => {
         val name = field.getName
         try {
           val getter = poso.getClass.getDeclaredMethod("get" + StringUtils.capitalize(name))
           val setter = poso.getClass.getDeclaredMethod("set" + StringUtils.capitalize(name), field.getType)
           Some((name -> ModelProperty(name, field.getType.getName, getter, setter)))
         } catch {
           case e: Exception => None
         }
       }).filter(x => !x.isEmpty).map(y => y.get).toMap

       posoLookupCache.put(poso.getClass, posoLookupMap)
       posoLookupMap
     } else {
       result.get
     }
   }
 }

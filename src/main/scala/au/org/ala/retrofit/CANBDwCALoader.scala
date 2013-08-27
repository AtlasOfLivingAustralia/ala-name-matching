package au.org.ala.retrofit

import au.org.ala.util.{OptionParser, DwCALoader}
import org.gbif.dwc.text.StarRecord
import org.gbif.dwc.terms.ConceptTerm
import au.org.ala.biocache.Config
import au.com.bytecode.opencsv.CSVReader
import java.io.{FileInputStream, InputStreamReader}
import collection.mutable

/*
 * Copyright (C) 2012 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */


object CANBDwCALoader{


  def main(args: Array[String]): Unit = {

    var resourceUid = "dr376"
    var localFilePath:Option[String] = None
    var mapFilePath:Option[String] = None
    var logRowKeys = false;
    var testFile =false
    val parser = new OptionParser("load CANB darwin core archive") {
      //arg("<data resource UID>", "The UID of the data resource to load", {v: String => resourceUid = v})
      opt("l", "local", "skip the download and use local file", {v:String => localFilePath = Some(v) } )
      opt("mf","mapFile","The mapping file for non default catalogue numbers", {v:String => mapFilePath = Some(v)})
      opt("log","log row keys to file - allows processing/indexing of changed records",{logRowKeys = true})
      opt("test", "test the file only do not load", {testFile=true})
    }
    if(parser.parse(args)){
      val l = new CANBDwCALoader
      l.deleteOldRowKeys(resourceUid)
      if(localFilePath.isDefined){
        if(mapFilePath.isDefined){
          l.setNonDefaultMapping(mapFilePath.get)
          l.loadLocal(resourceUid, localFilePath.get, logRowKeys, testFile)
        }
        l.updateLastChecked(resourceUid)
      }
//      val l = new DwCALoader
//      l.deleteOldRowKeys(resourceUid)
//      if(localFilePath.isEmpty){
//        l.load(resourceUid, logRowKeys,testFile)
//      } else {
//        l.loadLocal(resourceUid, localFilePath.get, logRowKeys,testFile)
//      }
//      //initialise the delete
//      //update the collectory information
//      l.updateLastChecked(resourceUid)
    }

    //shut down the persistence manager after all the files have been loaded.
    Config.persistenceManager.shutdown
  }

}

/**
 * Retrofiting the new CANB catalogue numbers with the old UUIDs
 */
class CANBDwCALoader extends DwCALoader{

  val nonDefaultMapping:scala.collection.mutable.HashMap[String,String]= new scala.collection.mutable.HashMap[String,String]

  def setNonDefaultMapping(file:String){
    val reader =  new CSVReader(new InputStreamReader(new org.apache.commons.io.input.BOMInputStream(new FileInputStream(file))), ',', '"', '~')
    reader.readNext() //header line
    var currentLine = reader.readNext //first line

    while(currentLine != null){

      if(currentLine.size>1){
        nonDefaultMapping += (currentLine(0)->currentLine(1))
      }
      currentLine = reader.readNext()
    }

    println(nonDefaultMapping)
  }
  val list=List("firstLoaded", "outlierForLayers.p" , "duplicationStatus.p"  ,"duplicationType.p" ,"associatedOccurrences.p"  ,"qualityAssertion",  "userQualityAssertion")
  /**
   * override so that we can have the very customised mapping to get the correct value
   * @param star
   * @param uniqueTerms
   * @return
   */
  override def getUuid(uniqueID:Option[String], star:StarRecord,uniqueTerms:List[ConceptTerm], mappedProperties:Option[Map[String,String]]) :((String, Boolean), Option[Map[String,String]])={
    //Rule: If the catalogue number suffix is .1 OR is in the supplied CSV then we remove the suffix before we test for the UUID

    val newUniqueId = {
      if (!uniqueTerms.isEmpty) {
        val uniqueTermValues = uniqueTerms.map(t => getRetrofitValue(t,star.core.value(t)))
        val id =(List("dr376") ::: uniqueTermValues).mkString("|").trim
        //we always strip the spaces for AVH dr376
        Some(id.replaceAll("\\s",""))
      } else {
        None
      }
    }
    val props=Config.persistenceManager.get(newUniqueId.get,"occ")
    var mappedProps:Option[Map[String,String]]=None
    if(props.isDefined){
      val map = new mutable.HashMap[String,String]()
      val sublist =list.filter(i =>props.get.contains(i))
      sublist.foreach(i => map +=(i-> props.get.getOrElse(i, "")))
      mappedProps = Some(map.toMap)
    }
    //

    super.getUuid(newUniqueId, star,uniqueTerms,mappedProps)
  }
  def getRevisedCatalogueNumber(value:String):String ={
    if(value.endsWith((".1")) || nonDefaultMapping.get(value).isDefined){
      value.substring(0,value.lastIndexOf("."))
    }  else {
      value
    }
  }
  def getRetrofitValue(field:ConceptTerm, value:String):String ={
    if(field.simpleName() == "catalogNumber"){
      //check to see if the suffix needs to be removed
      getRevisedCatalogueNumber(value)
    } else {
      value
    }
  }
}

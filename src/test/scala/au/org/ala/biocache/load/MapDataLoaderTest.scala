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
package au.org.ala.biocache.load

import au.org.ala.biocache.{Config, ConfigFunSuite}
import java.util
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MapDataLoaderTest extends ConfigFunSuite {
  test("map load with dwc substitution"){
    val loader = new MapDataLoader
    val map = Map("occurrenceId"->"myid","scientificName"->"Macropus rufus","eventDate"->"2014-04-01","imageLicence"->"CC", "commonName"->"Red Kangaroo")
    val jmap = new util.HashMap[String,String]()
    map.foreach{case(k,v)=> jmap.put(k,v)}
    loader.load("drnq",List(jmap),List("occurrenceId"))
    println(Config.persistenceManager.get("drnq|myid","occ"))
    val rights= Config.persistenceManager.get("drnq|myid","occ","rights")
    expectResult(Some("CC")){rights}
    expectResult(Some("Red Kangaroo")){Config.persistenceManager.get("drnq|myid","occ","vernacularName")}
  }
}

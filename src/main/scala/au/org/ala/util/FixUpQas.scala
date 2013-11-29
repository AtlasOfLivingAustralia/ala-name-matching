package au.org.ala.util

import au.org.ala.biocache.Config
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import scala.collection.{mutable, JavaConversions}
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.commons.io.IOUtils

object FixUpQas {

  def main(args:Array[String]){

    import JavaConversions._

    val lookupUrl = "http://auth.ala.org.au/userdetails/userDetails/getUserListFull"
    val om = new ObjectMapper
    val httpClient = new DefaultHttpClient
    val httpPost = new HttpPost(lookupUrl)
    val resp = httpClient.execute(httpPost)
    val postBody = IOUtils.toString(resp.getEntity().getContent)
    val jsonNode:JsonNode = om.readTree(postBody)
    val userMap = new mutable.HashMap[String,String]

    jsonNode.elements().foreach( node => {
      val email = node.get("email").textValue().toLowerCase()
      val id = node.get("id").intValue().toString()
      println(email + ":" + id)
      userMap.put(email, id)
    })

    //paging through queryassert
    Config.persistenceManager.pageOverAll("queryassert", (key,map) => {
      println(key)
      val userEmail =  map.getOrElse("userName", "")
      Config.persistenceManager.put(key, "queryassert", "userEmail", userEmail)

      //lookup the CAS ID for this user..
      val id = userMap.get(userEmail)
      if(!id.isEmpty){
        Config.persistenceManager.put(key, "queryassert", "userId", id.get)
      }
      true
    })

    //paging through qa
    Config.persistenceManager.pageOverAll("qa", (key,map) => {

      println(key)

      val userEmail =  map.getOrElse("userId", "")
      Config.persistenceManager.put(key, "qa", "userEmail", userEmail)

      //lookup the CAS ID for this user..
      val id = userMap.get(userEmail)
      if(!id.isEmpty){
        Config.persistenceManager.put(key, "qa", "userId", id.get)
      }

      true
    })
    Config.persistenceManager.shutdown
  }
}

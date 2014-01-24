package au.org.ala.util
import au.org.ala.biocache.Json
import scalaj.http.Http
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.HttpClient
import au.org.ala.biocache.Config

/**
 * Load a temp resource
 */
object TempDataLoader {

  //create a temp data resource
  def main(args: Array[String]): Unit = {

    var userId = ""
    var apiKey = ""
    var name = ""

    val parser = new OptionParser("Create a temporary resource") {
      arg("<user>", "The user uploading", { v: String => userId = v })
      arg("<api_key>", "The api_key to use", { v: String => apiKey = v })
      arg("<dataset_name>", "The name to use", { v: String => name = v })
    }
    if (parser.parse(args)) {
      val map = Map("user" -> userId, "api_key" -> apiKey, "numberOfRecords" -> "0", "name" -> name)
      val data: String = Json.toJSON(map)
      println(data)
      
      val http = new HttpClient()
      val post = new PostMethod(Config.registryURL + "/tempDataResource")
      post.setRequestBody(data)
      http.executeMethod(post)
      post.getResponseHeaders().foreach(h => println(h.getName() + ": " +h.getValue()))
      println(post.getRequestHeader("location"))
      //println(request.responseCode)
    }
  }
}
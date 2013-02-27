package au.org.ala.util

import au.org.ala.biocache.DataLoader
import com.gargoylesoftware.htmlunit.{Page, WebRequestSettings, BrowserVersion, WebClient}
import java.net.URL
import com.gargoylesoftware.htmlunit.html.{HtmlHead, HtmlPage}
import xml.{Node, Elem, XML}
import java.text.MessageFormat

/**
 * Created with IntelliJ IDEA.
 * User: ChrisF
 * Date: 27/02/13
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
object NatureShareLoader extends DataLoader {

  val INDEX_PAGE = "http://natureshare.org.au/observation/browse_by_created/?mode=basic"
  val OBSERVATION_PAGE_TEMPLATE = "http://natureshare.org.au/observation/{0}/"

  def main(args: Array[String]) {
    new NatureShareLoader().load("foo")
  }

  def getHTMLPageAsXML(url: String): Elem = {
    val webClient = new WebClient(BrowserVersion.FIREFOX_3);

    // Disable Javascript as basically we don't need it.
    webClient.setJavaScriptEnabled(false);

    val targetPageUrl = new URL(url);
    val reqSettings = new WebRequestSettings(targetPageUrl);
    reqSettings.setCharset("UTF-8");

    try {
      val currentPage: HtmlPage = webClient.getPage(reqSettings);

      webClient.closeAllWindows();
      currentPage.cleanUp();
      val idMetaTagElement = currentPage.createElement("meta");
      idMetaTagElement.setAttribute("name", "ALA.Guid");
      idMetaTagElement.setAttribute("scheme", "URL");
      idMetaTagElement.setAttribute("content", url);

      // this.currentPage.appendChild(idMetaTagElement);
      val rootElement = currentPage.getDocumentElement();
      val headSection = rootElement.getByXPath("//html/head").asInstanceOf[java.util.List[HtmlHead]];
      val currentHtmlHead: HtmlHead = headSection.get(0);
      currentHtmlHead.appendChild(idMetaTagElement);

      XML.loadString(currentPage.asXml());
    } catch {
      case ex: Exception => {
        logger.error(ex.getMessage(), ex);
      }

      return null;
    }
  }
}


class NatureShareLoader extends CustomWebserviceLoader {
  def load(dataResourceUid: String) {

    val xml = NatureShareLoader.getHTMLPageAsXML(NatureShareLoader.INDEX_PAGE)
    val pageLinks = xml \\ "@href"
    val firstLink = pageLinks.find(_.toString().startsWith("/observation/")).get

    val latestObservationNumber = firstLink.toString().split("/")(2)
    processObservation(latestObservationNumber)
  }

  def processObservation(observationNumber: String) {
    val xml = NatureShareLoader.getHTMLPageAsXML(MessageFormat.format(NatureShareLoader.OBSERVATION_PAGE_TEMPLATE, observationNumber))

    val divs = xml \\ "div"

    val photosDiv = divs.find(nodeContainsAttributeWithValue(_, "id", "photos"))
    println(photosDiv)

    // scrape contributor

    // scrape photos

    // scrape species

    // scrape tags

    // scrape date/time

    // scrape description

    // scrape collections

    // scrape location

    // scrape comments

    // scrape parts of the metadata

    // All records have licence CC BY 2.5 AU
  }

  def nodeContainsAttributeWithValue(node: Node, key: String, value: String): Boolean = {
    val attributeNodes = node.attribute(key).getOrElse(null)
    if (attributeNodes != null) {
      if (!attributeNodes.filter(_.text == value).isEmpty) {
        true
      } else {
        false
      }
    } else {
      false
    }
  }
}
package au.org.ala.biocache.load

abstract class CustomWebserviceLoader extends DataLoader {
  def load(dataResourceUid: String): Unit
}


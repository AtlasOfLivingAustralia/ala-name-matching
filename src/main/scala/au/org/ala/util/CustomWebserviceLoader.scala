package au.org.ala.util

import au.org.ala.biocache.DataLoader

abstract class CustomWebserviceLoader extends DataLoader {
  def load(dataResourceUid: String): Unit
}


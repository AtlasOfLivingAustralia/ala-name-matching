
package org.ala.biocache.util;

import java.io.InputStream;
import java.util.Properties;
import org.springframework.stereotype.Component;

/**
 *
 *  Stores the download fields whose values can be overridden in
 * a properties file
 *
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 */

public class DownloadFields {
    
private String defaultFields="uuid,catalogNumber,taxonConceptID.p,scientificName,vernacularName,scientificName.p,taxonRank.p,vernacularName.p,kingdom.p,phylum.p,classs.p,order.p,family.p,genus.p,species.p,subspecies.p,institutionCode,collectionCode,latitude.p,longitude.p,coordinatePrecision,country.p,ibra.p,imcra.p,stateProvince.p,lga.p,minimumElevationInMeters,maximumElevationInMeters,minimumDepthInMeters,maximumDepthInMeters,year.p,month.p,day.p,eventDate.p,eventTime.p,basisOfRecord,sex,preparations";
private Properties downloadProperties;
public DownloadFields(){
    //initialise the properties
    try{
        downloadProperties = new Properties();
        InputStream is = getClass().getResourceAsStream("/download.properties");
        downloadProperties.load(is);
    }
    catch(Exception e){
        e.printStackTrace();
    }
    if(downloadProperties.getProperty("fields") == null)
        downloadProperties.setProperty("fields", defaultFields);

}
/**
 * Get the name of the field that should be included in the download.
 * @return
 */
public String getFields(){
    return downloadProperties.getProperty("fields");
}
/**
 * Gets the header for the file
 * @param values
 * @return
 */
public String[] getHeader(String[] values){
    String[] header = new String[values.length];
    for(int i =0 ;i<values.length;i++){
        //attempt to get the headervalue from the propreties
        String v = downloadProperties.getProperty(values[i]);
        header[i] = v!=null ?v :values[i];
    }
    return header;
}

}

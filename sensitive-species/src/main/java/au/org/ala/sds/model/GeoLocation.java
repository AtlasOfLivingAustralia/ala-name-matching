/**
 * 
 */
package au.org.ala.sds.model;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeoLocation {
    private String latitude;
    private String longitude;
    
    public GeoLocation(String latitude, String longitude) {
        super();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }
    
    @Override
    public String toString() {
        return latitude + "," + longitude;
    }

}

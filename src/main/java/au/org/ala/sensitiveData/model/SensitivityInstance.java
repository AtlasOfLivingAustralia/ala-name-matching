package au.org.ala.sensitiveData.model;

/**
*
* @author Peter Flemming (peter.flemming@csiro.au)
*/
public class SensitivityInstance {

    private ConservationCategory category;
    private String dataProvider;
    private SensitivityZone zone;
    
    public SensitivityInstance(ConservationCategory category, String dataProvider, SensitivityZone zone) {
        this.category = category;
        this.dataProvider = dataProvider;
        this.zone = zone;
    }

    public ConservationCategory getCategory() {
        return category;
    }

    public String getDataProvider() {
        return dataProvider;
    }

    public SensitivityZone getZone() {
        return zone;
    }

    
}

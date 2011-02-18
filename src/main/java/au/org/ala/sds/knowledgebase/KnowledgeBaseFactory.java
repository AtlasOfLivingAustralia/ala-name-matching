/**
 * 
 */
package au.org.ala.sds.knowledgebase;

import java.util.HashMap;
import java.util.Map;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.conf.SequentialOption;
import org.drools.io.ResourceFactory;

import au.org.ala.sds.model.SensitivityCategory;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class KnowledgeBaseFactory {

    static private Map<String, String> rules  = new HashMap<String, String>();
    static {
        rules.put(SensitivityCategory.PLANT_PEST_NOT_KNOWN_IN_AUSTRALIA.getValue(), "PBC1-PestNotKnownInAustralia.drl");
        rules.put(SensitivityCategory.PLANT_PEST_ERADICATED.getValue(), "PBC2-PestEradicated.drl");
        rules.put(SensitivityCategory.PLANT_PEST_UNDER_ERADICATION.getValue(), "PBC3-PestUnderEradication.drl");
        rules.put(SensitivityCategory.PLANT_PEST_SUBJECT_TO_OFFICIAL_CONTROL.getValue(), "PBC4-PestUnderOfficialControl.drl");
        rules.put(SensitivityCategory.PLANT_PEST_IN_QUARANTINE_OR_OTHER_PLANT_HEALTH_ZONE.getValue(), "PBC5-PestInQuarantineOrOtherPlantHealthZone.drl");
        rules.put(SensitivityCategory.PLANT_PEST_NOTIFIABLE_UNDER_STATE_LEGISLATION.getValue(), "PBC6-PestNotifiableUnderStateLegislation.drl");
    }
    
    public static KnowledgeBase getKnowledgeBase(String category) {
        KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        builder.add(ResourceFactory.newClassPathResource("PBC1-PestNotKnownInAustralia.drl"), ResourceType.DRL);
        if (builder.hasErrors()) {
            throw new RuntimeException(builder.getErrors().toString());
        }

        KnowledgeBaseConfiguration configuration = org.drools.KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        configuration.setOption(SequentialOption.YES);

        KnowledgeBase knowledgeBase = org.drools.KnowledgeBaseFactory.newKnowledgeBase(configuration);
        knowledgeBase.addKnowledgePackages(builder.getKnowledgePackages());
        
        return knowledgeBase;
    }
}

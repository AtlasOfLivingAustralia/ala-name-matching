/**
 *
 */
package au.org.ala.sds.knowledgebase;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
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

    protected static final Logger logger = Logger.getLogger(KnowledgeBaseFactory.class);

    static private Map<SensitivityCategory, String> rules  = new HashMap<SensitivityCategory, String>();
    static {
        rules.put(SensitivityCategory.PLANT_PEST_NOT_KNOWN_IN_AUSTRALIA, "PBC1-PlantPestNotKnownInAustralia.drl");
        rules.put(SensitivityCategory.PLANT_PEST_ERADICATED, "PBC2-PlantPestEradicated.drl");
        rules.put(SensitivityCategory.PLANT_PEST_UNDER_ERADICATION, "PBC3-PlantPestUnderEradication.drl");
        rules.put(SensitivityCategory.PLANT_PEST_SUBJECT_TO_OFFICIAL_CONTROL, "PBC4-PlantPestSubjectToOfficialControl.drl");
        rules.put(SensitivityCategory.PLANT_PEST_IN_TORRES_STRAIT_ZONE, "PBC5a-PlantPestInTorresStraitZone.drl");
        rules.put(SensitivityCategory.PLANT_PEST_IS_QUEENSLAND_FRUIT_FLY, "PBC5b-PlantPestIsQueenslandFruitFly.drl");
        rules.put(SensitivityCategory.PLANT_PEST_NOTIFIABLE_UNDER_STATE_LEGISLATION, "PBC6-PlantPestNotifiableUnderStateLegislation.drl");
    }
    static private Map<SensitivityCategory, KnowledgeBase> kbs = new HashMap<SensitivityCategory, KnowledgeBase>();

    public static KnowledgeBase getKnowledgeBase(SensitivityCategory category) {
        KnowledgeBase knowledgeBase;

        if ((knowledgeBase = kbs.get(category)) == null) {
            logger.debug("Instantiating KnowledgeBase '" + rules.get(category) + "'");
            KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
            builder.add(ResourceFactory.newClassPathResource(rules.get(category)), ResourceType.DRL);
            if (builder.hasErrors()) {
                throw new RuntimeException(builder.getErrors().toString());
            }

            KnowledgeBaseConfiguration configuration = org.drools.KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
            configuration.setOption(SequentialOption.YES);

            knowledgeBase = org.drools.KnowledgeBaseFactory.newKnowledgeBase(configuration);
            knowledgeBase.addKnowledgePackages(builder.getKnowledgePackages());
            kbs.put(category, knowledgeBase);
        }

        logger.debug("Using KnowledgeBase '" + rules.get(category) + "'");

        return knowledgeBase;
    }
}

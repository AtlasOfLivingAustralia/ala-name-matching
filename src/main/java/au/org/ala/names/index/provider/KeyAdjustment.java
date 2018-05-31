package au.org.ala.names.index.provider;

import au.org.ala.names.index.NameKey;
import au.org.ala.names.index.TaxonConceptInstance;
import au.org.ala.names.model.RankType;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalCode;

import javax.annotation.Nullable;

/**
 * A name key adjustment.
 * <p>
 * If the condition is met, replace any components of a {@link NameKey} specified in this adjustment.
 * For example, if the authorship is "Engl." and the name key is `<code>BOTANTICAL:FUNGI:Whittaker:SCIENTIFIC:KINGDOM</code>
 * the resulting name key will be <code>BOTANTICAL:FUNGI:Engl.:SCIENTIFIC:KINGDOM</code>
 * </p>
 * <p>
 * For the {@link au.org.ala.names.index.ALANameAnalyser}, Scientific names are expected to be all upper case.
 * </p>
 * <p>
 * If the {@link #scientificNameAuthorship} is an empty string, then the authorship is replaced with null.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class KeyAdjustment {
    /** The condition */
    @JsonProperty
    private TaxonCondition condition;
    /** The replacement nomenclaturalCode */
    @JsonProperty
    private NomenclaturalCode nomenclaturalCode;
    /** The replacement scientific name */
    @JsonProperty
    private String scientificName;
    /** The replacement scientific name authorship */
    @JsonProperty
    private String scientificNameAuthorship;
    /** The replacement name type */
    @JsonProperty
    private NameType nameType;
    /** The replacement rank */
    @JsonProperty
    private RankType rank;

    /**
     * Empty constructor
     */
    public KeyAdjustment() {
    }

    /**
     * Construct a key adjustment
     *
     * @param condition The condition
     * @param nomenclaturalCode The new nomenclatural code, or null for do not replace
     * @param scientificName The new scientific name, or null for do not replace. Must be uppercase.
     * @param scientificNameAuthorship The new authorship, an emptry string for replace with null or null for do not replace
     * @param nameType The new name type, or null for do not replace
     * @param rank The new rank, or null for do not replace
     */
    public KeyAdjustment(TaxonCondition condition, @Nullable NomenclaturalCode nomenclaturalCode, @Nullable String scientificName, @Nullable String scientificNameAuthorship, @Nullable NameType nameType, @Nullable RankType rank) {
        this.condition = condition;
        this.nomenclaturalCode = nomenclaturalCode;
        this.scientificName = scientificName;
        this.scientificNameAuthorship = scientificNameAuthorship;
        this.nameType = nameType;
        this.rank = rank;
    }

    public NameKey adjust(NameKey key, TaxonConceptInstance instance) {
        if (!this.condition.match(instance, key))
            return key;
        NomenclaturalCode nc = this.nomenclaturalCode != null ? this.nomenclaturalCode : key.getCode();
        String sn = this.scientificName != null ? this.scientificName : key.getScientificName();
        String sna = this.scientificNameAuthorship  != null ? this.scientificNameAuthorship : key.getScientificNameAuthorship();
        if (sna != null && sna.isEmpty())
            sna = null;
        NameType nt = this.nameType != null ? this.nameType : key.getType();
        RankType rt = this.rank != null ? this.rank : key.getRank();
        return new NameKey(key.getAnalyser(), nc, sn, sna, rt, nt);
    }
}

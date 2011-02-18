package au.org.ala.sds.validation;

import java.util.Date;

import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.model.SensitivityZone;

public interface ValidationService {

    ValidationOutcome validate(SensitiveSpecies ss, SensitivityZone act, Date date);

}

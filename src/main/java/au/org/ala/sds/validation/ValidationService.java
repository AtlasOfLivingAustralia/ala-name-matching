package au.org.ala.sds.validation;

import au.org.ala.sds.model.SensitiveTaxon;

public interface ValidationService {

    ValidationOutcome validate(SensitiveTaxon sensitiveSpecies, FactCollection facts);

}

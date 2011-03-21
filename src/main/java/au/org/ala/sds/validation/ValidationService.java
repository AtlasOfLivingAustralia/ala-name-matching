package au.org.ala.sds.validation;

import au.org.ala.sds.model.SensitiveSpecies;

public interface ValidationService {

    ValidationOutcome validate(SensitiveSpecies sensitiveSpecies, FactCollection facts);

}

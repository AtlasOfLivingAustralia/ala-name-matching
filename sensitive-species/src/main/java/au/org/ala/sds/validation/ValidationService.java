package au.org.ala.sds.validation;

import java.util.Map;


public interface ValidationService {

    ValidationOutcome validate(Map<String, String> biocacheData);

}

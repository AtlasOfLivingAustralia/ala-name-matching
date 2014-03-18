package au.org.ala.sds.validation;

import au.org.ala.sds.model.SensitiveTaxon;

public interface ReportFactory {

    ValidationReport createValidationReport(SensitiveTaxon taxon);

}

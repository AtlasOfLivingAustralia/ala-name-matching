package au.org.ala.sds.validation;

import au.org.ala.sds.model.SensitiveTaxon;

public class SdsReportFactory implements ReportFactory {

    public ValidationReport createValidationReport(SensitiveTaxon taxon) {
        return new SdsValidationReport(taxon);
    }

}

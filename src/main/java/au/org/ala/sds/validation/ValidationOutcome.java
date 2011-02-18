/**
 * 
 */
package au.org.ala.sds.validation;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ValidationOutcome {

        private ValidationReport report;
        
        private boolean loadable;
        
        private String annotation;

        public ValidationOutcome(ValidationReport report, boolean loadable) {
            super();
            this.report = report;
            this.loadable = loadable;
        }

        public ValidationReport getReport() {
            return report;
        }

        public void setReport(ValidationReport report) {
            this.report = report;
        }

        public boolean isLoadable() {
            return loadable;
        }

        public void setLoadable(boolean loadable) {
            this.loadable = loadable;
        }

        public String getAnnotation() {
            return annotation;
        }

        public void setAnnotation(String annotation) {
            this.annotation = annotation;
        }
}

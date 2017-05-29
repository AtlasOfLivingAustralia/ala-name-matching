package au.ala.org.vocab;

import org.gbif.dwc.terms.Term;

/**
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;

 * Copyright (c) 2016 CSIRO
 */
public enum ALATerm implements Term {
    nameComplete,
    nameFormatted,
    nameID,
    status;

    public static final String NS = "http://ala.org.au/terms/1.0/";
    public static final String PREFIX = "ala:";

    public String qualifiedName() {
        return NS + this.simpleName();
    }

    public String simpleName() {
        return this.name();
    }

    public String toString() {
         return PREFIX + name();
    }

}

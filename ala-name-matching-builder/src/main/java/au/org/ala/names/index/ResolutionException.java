/*
 * Copyright (c) 2021 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */

package au.org.ala.names.index;

import javax.annotation.Nullable;
import java.util.List;

/**
 * An exception caused by something like a synonym loop
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class ResolutionException extends IndexBuilderException {
    private List<TaxonomicElement> trace;

    public ResolutionException(String message, @Nullable List<TaxonomicElement> trace) {
        super(message);
        this.trace = trace;
    }

    public ResolutionException(String message, Throwable cause, @Nullable List<TaxonomicElement> trace) {
        super(message, cause);
        this.trace = trace;
    }

    public ResolutionException(String message) {
        super(message);
    }

    public ResolutionException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public List<TaxonomicElement> getTrace() {
        return trace;
    }
}

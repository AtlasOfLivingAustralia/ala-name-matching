/*
 * Copyright (c) 2022 Atlas of Living Australia
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * A simple implementation of a reporter for logging to a log stream.
 */
public class LogReporter implements Reporter {
    /**
     * The logger to report to
     */
    private static final Logger logger = LoggerFactory.getLogger(LogReporter.class);
    /**
     * The message format
     */
    private static final ResourceBundle resources = ResourceBundle.getBundle("taxonomy");

    /**
     * Default constructor.
     */
    public LogReporter() {
    }

    /**
     * Add a report.
     * <p>
     * Message codes are retrieved using a message bundle pointing to <code>taxonomy.properties</code>
     * Arguments are arbitrary strings and are not given specific meanings as in {@link #report(IssueType, String, TaxonomicElement, List<TaxonomicElement>)}.
     * </p>
     * <ul>
     *     <li>{0} The taxonID of the source element, either a name or a proper taxonID</li>
     *     <li>{1} Any attached scientific namer</li>
     *     <li>{2+} Any additional arguments</li>
     * </ul>
     *  @param type The type of report
     *
     * @param code    The message code to use for the readable version of the report
     * @param taxonID A specific taxonomic ID
     * @param name    A specific complete scientific name
     * @param args    Additional arguments for the report message
     */
    @Override
    public void report(IssueType type, String code, String taxonID, String name, String... args) {
        String message = this.format(code, taxonID, name, args.length > 0 ? args[0] : "", args.length > 1 ? args[1] : "", args.length > 2 ? args[2] : "");
        this.report(type, message);
        switch (type) {
            case ERROR:
            case VALIDATION:
                logger.error(message);
                break;
            case PROBLEM:
                logger.warn(message);
                break;
            case COLLISION:
                logger.info(message);
                break;
            case NOTE:
            case COUNT:
                logger.debug(message);
                break;
            default:
                logger.warn("Unknown message type " + type + ": " + message);
        }
    }

    /**
     * Add a report.
     * <p>
     * Message codes are retrieved using a message bundle pointing to <code>taxonomy.properties</code>
     * These are formatted with a message formatter and have the following arguments:
     * </p>
     * <ul>
     *     <li>{0} The taxonID of the first element, either a name or a proper taxonID</li>
     *     <li>{1} The scientific name of the first element</li>
     *     <li>{2} Any authorship of the first element</li>
     *     <li>{3} Any associated taxon ids</li>
     *     <li>{4} The first taxon element</li>
     *     <li>{5+} Any associated taxon elements</li>
     * </ul>
     *  @param type The type of report
     *
     * @param code       The message code to use for the readable version of the report
     * @param main       The main element
     * @param associated The elements that impact the report.
     */
    @Override
    public void report(IssueType type, String code, TaxonomicElement main, List<? extends TaxonomicElement> associated) {
        String taxonID = null;
        String associatedTaxa = "";
        String associatedDesc = "";
        String name = "";
        TaxonConceptInstance primary;

        if (main != null) {
            taxonID = main.getTaxonID();
            if (main.getDisplayName() != null)
                name = main.getDisplayName();
        }
        if (taxonID == null)
            taxonID = "";
        if (associated != null && !associated.isEmpty()) {
            StringBuilder ab = new StringBuilder();
            StringBuilder as = new StringBuilder();
            for (TaxonomicElement elt : associated) {
                if (ab.length() > 0) {
                    ab.append("|");
                    as.append(", ");
                }
                ab.append(elt.getTaxonID());
                as.append(elt.toString());
            }
            associatedTaxa = ab.toString();
            associatedDesc = as.toString();
        }
        String message = this.format(code, taxonID, name, associatedTaxa, main.toString(), associatedDesc);
        this.report(type, message);
    }

    /**
     * Format a string based on a message code.
     *
     * @param code The message code
     * @param taxonID The taxon ID
     * @param name The taxon name
     * @param associatedTaxa Any associated taxon identifiers
     * @param main The main concept
     * @param associatedDescription Any associated description
     *
     * @return A formatted version of the message
     */
    protected String format(String code, String taxonID, String name, String associatedTaxa, String main, String associatedDescription) {
        String message;
        String[] args = new String[5];
        args[0] = taxonID;
        args[1] = name;
        args[2] = associatedTaxa;
        args[3] = main;
        args[4] = associatedDescription;
        try {
            message = this.resources.getString(code);
            message = MessageFormat.format(message == null ? code : message, args);
        } catch (MissingResourceException ex) {
            logger.error("Can't find resource for " + code + " defaulting to code");
            message = code + " " + args;
        }
        return message;
    }

    /**
     * Report an issue to the log
     *
     * @param type The type of issue
     * @param message The log message
     */
    protected void report(IssueType type, String message) {
        switch (type) {
            case ERROR:
            case VALIDATION:
                logger.error(message);
                break;
            case PROBLEM:
                logger.warn(message);
                break;
            case COLLISION:
                logger.debug(message);
                break;
            case NOTE:
            case COUNT:
                logger.debug(message);
                break;
            default:
                logger.warn("Unknown message type " + type + ": " + message);
        }
    }
}

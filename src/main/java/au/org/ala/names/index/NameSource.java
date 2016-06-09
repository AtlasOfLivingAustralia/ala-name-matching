package au.org.ala.names.index;

import org.apache.lucene.document.Document;

import java.util.Collection;

/**
 * A source of name information.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *
 * Copyright (c) 2016 CSIRO
 */
abstract public class NameSource implements Iterable<Collection<Document>> {
    /** The source identifier */
    private String id;
    /** The source priority */
    private float priority;

    /**
     * Create a name source.
     *
     * @param id The source identifier
     * @param priority The source priority
     */
    public NameSource(String id, float priority) {
        this.id = id;
        this.priority = priority;
    }

    /**
     * Get the source identifier.
     *
     * @return The source ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the source priority
     *
     * @return The priority
     */
    public float getPriority() {
        return priority;
    }
}

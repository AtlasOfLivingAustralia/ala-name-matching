package au.org.ala.names.index;

import org.apache.lucene.document.Document;

import java.util.Collection;
import java.util.UUID;

/**
 * A provider of name information.
 * <p>
 * Providers can be used to prioritise clashing names.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *
 * Copyright (c) 2016 CSIRO
 */
public class NameProvider {
    /** The provider identifier */
    private String id;
    /** The source priority */
    private float priority;

    /**
     * Default constructor
     */
    public NameProvider() {
        this.id = UUID.randomUUID().toString();
        this.priority = 1.0f;
    }

    /**
     * Create a name source.
     *
     * @param id The source identifier
     * @param priority The source priority
     */
    public NameProvider(String id, float priority) {
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

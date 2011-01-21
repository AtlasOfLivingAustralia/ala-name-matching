/**************************************************************************
 *  Copyright (C) 2009 Atlas of Living Australia
 *  All Rights Reserved.
 *  
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *  
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package org.ala.biocache.dto;

import java.util.Date;

import org.ala.biocache.dto.OccurrenceAnnotationBody;

/**
 * DTO bean to represent a "field update" annotation to an occurrence record.
 * @see org.ala.dto.OccurrenceAnnotation
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class OccurrenceAnnotation {
    private String annotates;
    private String inReplyTo;
    private String replyField;
    private String bodyUrl;
    private String creator;
    private String annoteaKey;
    private Date date;
    private OccurrenceAnnotationBody body;

    @Override
    public String toString() {
        return "annotates: "+annotates+"; inReplyTo: "+inReplyTo+"; replyField: "+replyField+"; bodyUrl: "+bodyUrl+"; creator: "+
                creator+"; key: "+annoteaKey+"; date: "+date; //+"; body: ["+body.toString()+"]";
    }

    public String getAnnotates() {
        return annotates;
    }

    public void setAnnotates(String annotates) {
        this.annotates = annotates;
    }

    public OccurrenceAnnotationBody getBody() {
        return body;
    }

    public void setBody(OccurrenceAnnotationBody body) {
        this.body = body;
    }

    public String getBodyUrl() {
        return bodyUrl;
    }

    public void setBodyUrl(String bodyUrl) {
        this.bodyUrl = bodyUrl;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getAnnoteaKey() {
        return annoteaKey;
    }

    public void setAnnoteaKey(String annoteaKey) {
        this.annoteaKey = annoteaKey;
    }

    public String getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    public String getReplyField() {
        return replyField;
    }

    public void setReplyField(String replyField) {
        this.replyField = replyField;
    }

}

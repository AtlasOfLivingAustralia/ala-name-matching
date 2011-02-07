/* *************************************************************************
 *  Copyright (C) 2011 Atlas of Living Australia
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

import java.io.Serializable;
import java.util.Date;
import org.ala.biocache.web.CustomDateSerializer;
import org.apache.solr.client.solrj.beans.Field;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * A useless Java bean to test nested classes in JSON marshalling and un-marshalling
 * 
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class UselessBean implements Serializable {
    private static final long serialVersionUID = -4501626905355608086L;
    @Field protected String name;
    @Field protected Integer age;    
    @Field("creation_date") protected Date creationDate;

    public UselessBean(String name, Integer age) {
        this.name = name;
        this.age = age;
        this.creationDate = new Date();
    }
    
    public UselessBean() {}

    @Override
    public String toString() {
        return "UselessBean{" + "name=" + name + ", age=" + age + ", creationDate=" + creationDate + '}';
    }

    /**
     * Get the value of name
     *
     * @return the value of name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the value of name
     *
     * @param name new value of name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the value of age
     *
     * @return the value of age
     */
    public Integer getAge() {
        return age;
    }

    /**
     * Set the value of age
     *
     * @param age new value of age
     */
    public void setAge(Integer age) {
        this.age = age;
    }

    /**
     * Get the value of creationDate
     *
     * @return the value of creationDate
     */
    @JsonSerialize(using = CustomDateSerializer.class)
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Set the value of creationDate
     *
     * @param creationDate new value of creationDate
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    
    @JsonIgnore
    public String getMissingProp() {
        return "missing"; 
    }

}

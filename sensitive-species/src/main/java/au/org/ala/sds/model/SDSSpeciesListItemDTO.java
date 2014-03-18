/*
 * Copyright (C) 2012 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */
package au.org.ala.sds.model;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

/**
 * DTO to represent a species list item.  It includes all the unique information necessary for a sensitive instance.
 *
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
public class SDSSpeciesListItemDTO {
    private String guid;
    private String name;
    private String family;
    private String dataResourceUid;
    private List<Map<String, String>> kvpValues;
    public static final List<String> commonNameLabels= Lists.newArrayList("commonname","vernacularname");

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataResourceUid() {
        return dataResourceUid;
    }

    public void setDataResourceUid(String dataResourceUid) {
        this.dataResourceUid = dataResourceUid;
    }

    public List<Map<String, String>> getKvpValues() {
        return kvpValues;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public void setKvpValues(List<Map<String, String>> kvpValues) {
        this.kvpValues = kvpValues;
    }
    public String getKVPValue(String key){
        for(Map<String, String> pair: kvpValues){
            if(key.equals(pair.get("key"))){
                return pair.get("value");
            }
        }
        return null;
    }
    public String getKVPValue(List<String> keys){
        for(Map<String, String> pair: kvpValues){
            if(keys.contains(pair.get("key").toLowerCase().replaceAll(" ", ""))){
                return pair.get("value");
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "SDSSpeciesListItemDTO{" +
                "guid='" + guid + '\'' +
                ", name='" + name + '\'' +
                ", dataResourceUid='" + dataResourceUid + '\'' +
                ", kvpValues=" + kvpValues +
                ", family=" + family +
                '}';
    }
}

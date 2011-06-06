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

/**
 * Data Transfer Object to represent the request parameters required to perform
 * a spatial search on occurrence records against biocache-service.
 *
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 */
public class SpatialSearchRequestParams extends SearchRequestParams{
    protected Float radius =5f;
    protected Float lat = -35.27412f;
    protected Float lon = 149.11288f;
    protected String wkt ="";

    /**
     * Custom toString method to produce a String to be used as the request parameters
     * for the Biocache Service webservices
     *
     * @return request parameters string
     */
    @Override
    public String toString() {
        StringBuilder req = new StringBuilder(super.toString());
        req.append("&lat=").append(lat);
        req.append("&lon=").append(lon);
        req.append("&radius=").append(radius);
        if(wkt.length() >0)
            req.append("&wkt=").append(wkt);
        return req.toString();
    }

    public Float getLat() {
        return lat;
    }

    public void setLat(Float lat) {
        this.lat = lat;
    }

    public Float getLon() {
        return lon;
    }

    public void setLon(Float lon) {
        this.lon = lon;
    }

    public Float getRadius() {
        return radius;
    }

    public void setRadius(Float radius) {
        this.radius = radius;
    }

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }
    
}

/* *************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
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

import java.util.ArrayList;
import java.util.List;


/**
 * Model object to store a list of occurrence cells
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class OccurrenceCell extends OccurrencePoint {
    protected List<List<Float>> coordinateCells = new ArrayList<List<Float>>(); // long, lat order

    public OccurrenceCell(OccurrencePoint op) {
        super();
        this.type = op.getType();
        this.count = op.getCount();
        this.coordinates = op.getCoordinates();
        this.createCellCoords();
    }

    public void createCellCoords() {
        // set top-left point
        coordinateCells.add(coordinates);
        // set top-right point
        List<Float> coords2 = new ArrayList<Float>();
        coords2.add(coordinates.get(0)+type.getValue());
        coords2.add(coordinates.get(1));
        coordinateCells.add(coords2);
        // set bottom-left point
        List<Float> coords3 = new ArrayList<Float>();
        coords3.add(coordinates.get(0)+type.getValue());
        coords3.add(coordinates.get(1)+type.getValue());
        coordinateCells.add(coords3);
         // set bottom-right point
        List<Float> coords4 = new ArrayList<Float>();
        coords4.add(coordinates.get(0));
        coords4.add(coordinates.get(1)+type.getValue());
        coordinateCells.add(coords4);
        // complete polygon back to top-left point
        coordinateCells.add(coordinates);
    }

    public List<List<Float>> getCoordinateCells() {
        return coordinateCells;
    }

    public void setCoordinateCells(List<List<Float>> coordinateCells) {
        this.coordinateCells = coordinateCells;
    }

}

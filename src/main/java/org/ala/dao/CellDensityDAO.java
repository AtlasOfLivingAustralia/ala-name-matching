/***************************************************************************
 * Copyright (C) 2009 Atlas of Living Australia
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
 ***************************************************************************/
package org.ala.dao;

import java.io.IOException;

import org.ala.io.CellDensityOutputStream;

/**
 * An optimised DAO layer for output cell density queries out to streams - avoiding any DTOs.
 * 
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public interface CellDensityDAO {

	public void outputCellDensities(long entityId, int type, CellDensityOutputStream output)
		throws IOException;
	
	public void outputCellDensities(long entityId, int type, int minCellId, int maxCellId, CellDensityOutputStream output)
		throws IOException;
	
	public void outputCentiCellDensities(long entityId, int type, CellDensityOutputStream output)
		throws IOException;
	
	public void outputCentiCellDensities(long entityId, int type, int minCellId, int maxCellId, CellDensityOutputStream output)
		throws IOException;

	public void outputTenMilliCellDensities(long entityId, int type, CellDensityOutputStream output)
		throws IOException;
	
	public void outputTenMilliCellDensities(long entityId, int type, int minCellId, int maxCellId, CellDensityOutputStream output)
		throws IOException;
}
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
package org.ala.io;

import java.io.IOException;
import java.io.OutputStream;

import org.gbif.portal.util.geospatial.CellIdUtils;
import org.gbif.portal.util.geospatial.LatLongBoundingBox;

/**
 * An output stream wrapper for outputting cell densities
 * in minx miny maxx maxy density format.
 *
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public class CellDensityOutputStream {
	
	protected OutputStream outputStream;
	
	protected byte[] delimiter = "\t".getBytes();
	protected byte[] eol = "\n".getBytes();
	
	/**
	 * Initialise this output stream.
	 * 
	 * @param outputStream
	 */
	public CellDensityOutputStream(OutputStream outputStream){
		this.outputStream = outputStream;
	}

	/**
	 * Output a cell density.
	 * 
	 * @param cellId
	 * @param count
	 * @throws IOException
	 */
	public void writeCell(int cellId, int count) throws IOException{
		LatLongBoundingBox llbb = CellIdUtils.toBoundingBox(cellId);
		writeOutLatLongBoundingBox(llbb,count);
		this.outputStream.flush();
	}

	/**
	 * Output a centi cell density.
	 * 
	 * @param cellId
	 * @param centiCellId
	 * @param count
	 * @throws IOException
	 */
	public void writeCentiCell(int cellId, int centiCellId, int count) throws IOException{
		LatLongBoundingBox llbb = CellIdUtils.toBoundingBox(cellId, centiCellId);
		writeOutLatLongBoundingBox(llbb,count);
		this.outputStream.flush();
	}
	
	/**
	 * Output a tenmilli cell density
	 * 
	 * @param cellId
	 * @param tenMilliCellId
	 * @param count
	 * @throws IOException
	 */
	public void writeTenMilliCell(int cellId, int tenMilliCellId, int count) throws IOException{
		LatLongBoundingBox llbb = tenMilliToBoundingBox(cellId, tenMilliCellId);
		writeOutLatLongBoundingBox(llbb, count);
		this.outputStream.flush();
	}

	/**
	 * Write out a LatLongBoundingBox to the stream
	 * @param llbb
	 * @param count
	 * @throws IOException
	 */
	private void writeOutLatLongBoundingBox(LatLongBoundingBox llbb, int count) throws IOException {
		outputStream.write(Float.toString(llbb.getMinLong()).getBytes());
		outputStream.write(delimiter);
		outputStream.write(Float.toString(llbb.getMinLat()).getBytes());
		outputStream.write(delimiter);
		outputStream.write(Float.toString(llbb.getMaxLong()).getBytes());
		outputStream.write(delimiter);
		outputStream.write(Float.toString(llbb.getMaxLat()).getBytes());
		outputStream.write(delimiter);
		outputStream.write(Integer.toString(count).getBytes());
		outputStream.write(eol);
	}
	
	/**
	 * Returns the box of the given cell and tenmilli cell.
	 * 
	 * @param cellId To return the lat long box of
	 * @param centiCellId within the box
	 * @param tenMilliCellId within the box
	 * @return The box
	 */
	public static LatLongBoundingBox tenMilliToBoundingBox(int cellId, int tenMilliCellId) {
		int longitudeX100 = 100*((cellId%360) - 180);
		int latitudeX100 = -900;
		if (cellId>0) {
			latitudeX100 = 100*(new Double(Math.floor(cellId/360)).intValue() - 90);
		}
		
		float longOffset = (tenMilliCellId%100);
		float latOffset = 0;
		if (tenMilliCellId>0){
			latOffset = tenMilliCellId/100;
		}
		
		float minLatitude = ((float)latitudeX100 + latOffset)/100;
		float minLongitude = ((float)longitudeX100 + longOffset)/100;
		float maxLatitude = ((float)latitudeX100 + latOffset + 1)/100;
		float maxlongitude = ((float)longitudeX100 + longOffset + 1)/100;
		return new LatLongBoundingBox(minLongitude, minLatitude, maxlongitude, maxLatitude);
	}
	
	/**
	 * @return the outputStream
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}

	/**
	 * @param outputStream the outputStream to set
	 */
	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}
}
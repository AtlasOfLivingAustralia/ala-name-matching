/**
 * 
 */
package org.ala.io;

import java.io.IOException;
import java.io.OutputStream;

import org.gbif.portal.util.geospatial.CellIdUtils;
import org.gbif.portal.util.geospatial.LatLongBoundingBox;


/**
 * 
 *
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public class CellDensityOutputStream {
	
	protected OutputStream outputStream;
	
	protected byte[] delimiter = "\t".getBytes();
	protected byte[] eol = "\n".getBytes();

	public CellDensityOutputStream(OutputStream outputStream){
		this.outputStream = outputStream;
	}

	public void writeCell(int cellId, int count) throws IOException{
		LatLongBoundingBox llbb = CellIdUtils.toBoundingBox(cellId);
		writeOutLatLongBoundingBox(llbb,count);
		this.outputStream.flush();
	}

	public void writeCentiCell(int cellId, int centiCellId, int count) throws IOException{
		LatLongBoundingBox llbb = CellIdUtils.toBoundingBox(cellId, centiCellId);
		writeOutLatLongBoundingBox(llbb,count);
		this.outputStream.flush();
	}	
	
	public void writeTenMilli(int cellId, int tenMilliCellId, int count) throws IOException{
		LatLongBoundingBox llbb = tenMilliToBoundingBox(cellId, tenMilliCellId);
		writeOutLatLongBoundingBox(llbb, count);
		this.outputStream.flush();
	}		

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
	 * Returns the box of the given cell and centi cell
	 * An attempt has been made to avoid rounding errors with floats, but may need revisited
	 * @param cellId To return the lat long box of
	 * @param centiCellId within the box
	 * @param tenMilliCellId within the box
	 * @return The box
	 */
	public static LatLongBoundingBox tenMilliToBoundingBox(int cellId, int tenMilliCellId) {
		int longitudeX10 = 10*((cellId%360) - 180);
		int latitudeX10 = -900;
		if (cellId>0) {
			latitudeX10 = 10*(new Double(Math.floor(cellId/360)).intValue() - 90);
		}
		
		float longOffset = (tenMilliCellId%10);
		float latOffset = 0;
		if (tenMilliCellId>0){
			latOffset = tenMilliCellId/10;
		}
		
		float minLatitude = (latitudeX10 + latOffset)/10;
		float minLongitude = (longitudeX10 + longOffset)/10;
		float maxLatitude = (latitudeX10 + latOffset + 1)/10;
		float maxlongitude = (longitudeX10 + longOffset + 1)/10;
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
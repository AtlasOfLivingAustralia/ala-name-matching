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
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.gbif.portal.util.geospatial.CellIdUtils;
import org.gbif.portal.util.geospatial.LatLongBoundingBox;

/**
 * An output stream wrapper for outputting cell densities
 * in minx miny maxx maxy density format.
 *
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public class KmlCellDensityOutputStream extends CellDensityOutputStream{
	
	protected Template hdr = null;
	protected Template ftr = null;
	protected Template bdy = null;
	
	protected String hostUrl;
	protected String fileName;
	
	/** The velocityContext to use */
	private VelocityContext velocityContext = new VelocityContext();
	/** The output stream wrapper to write to */
	private OutputStreamWriter writer = null;
	/** The Zipped outputstream to use */ 
	protected ZipOutputStream zout = null;

	/**
	 * Initialise this output stream.
	 * 
	 * @param outputStream
	 */
	public KmlCellDensityOutputStream(
			OutputStream outputStream,
			String entityId,
			String entityName,
			String entityType,
			String entityTypeName,
			String hostUrl,
			String fileName,
			boolean zipped) throws Exception {
		super(outputStream);
		if(zipped){
			this.zout = new ZipOutputStream(this.outputStream);
			this.zout.putNextEntry(new ZipEntry(fileName));
			this.outputStream = this.zout;
		}
		this.entityId = entityId;
		this.entityType = entityType;
		this.entityName = entityName;
		this.hostUrl= hostUrl;
		this.writer = new OutputStreamWriter(this.outputStream);
		init();
	}
	
	/**
	 * @throws Exception
	 */
	private void init() throws Exception {
		this.hdr = Velocity.getTemplate("org/ala/io/centiCellKmlHeader.vm");
		this.bdy = Velocity.getTemplate("org/ala/io/centiCellKml.vm");
		this.ftr = Velocity.getTemplate("org/ala/io/centiCellKmlFooter.vm");
		velocityContext.put("entityName", entityName);
		velocityContext.put("entityId", entityId);
		velocityContext.put("entityType", entityType);
		velocityContext.put("entityTypeName", entityTypeName);
		velocityContext.put("hostUrl", hostUrl);
		this.hdr.merge(velocityContext, writer);
		this.writer.flush();
	}

	/**
	 * @see org.ala.io.CellDensityOutputStream#close()
	 */
	@Override
	public void close() throws Exception {
		this.ftr.merge(velocityContext, writer);
		this.writer.flush();
		if(zout!=null){
			zout.closeEntry();
		}
		this.writer.close();
	}
	
	/**
	 * Write out a LatLongBoundingBox to the stream
	 * @param llbb
	 * @param count
	 * @throws IOException
	 */
	protected void writeOutLatLongBoundingBox(int cellId, int centiCellId, int tenMilliCellId, LatLongBoundingBox llbb, int count) throws IOException {
		try {
			velocityContext.put("entityId", entityId);
			velocityContext.put("entityName", entityName);
			velocityContext.put("entityType", entityType);
			velocityContext.put("entityTypeName", entityTypeName);
			velocityContext.put("llbb", llbb);
			velocityContext.put("latitude", (llbb.getMaxLat()+llbb.getMinLat())/2);
			velocityContext.put("longitude", (llbb.getMaxLong()+llbb.getMinLong())/2);
			if(cellId>=0)
				velocityContext.put("cellId", cellId);
			if(centiCellId>=0)
				velocityContext.put("centiCellId", centiCellId);
			if(tenMilliCellId>=0)
				velocityContext.put("tenMilliCellId", tenMilliCellId);
			velocityContext.put("density", count);
			this.bdy.merge(velocityContext, writer);
			this.writer.flush(); //flush every entry - keep buffer small
		} catch (Exception e){
			e.printStackTrace();
			throw new IOException (e.getMessage());
		}
	}
	
	/**
	 * Output a cell density.
	 * 
	 * @param cellId
	 * @param count
	 * @throws IOException
	 */
	@Override
	public void writeCell(int cellId, int count) throws IOException{
		LatLongBoundingBox llbb = CellIdUtils.toBoundingBox(cellId);
		writeOutLatLongBoundingBox(cellId,-1,-1,llbb,count);
	}

	/**
	 * Output a centi cell density.
	 * 
	 * @param cellId
	 * @param centiCellId
	 * @param count
	 * @throws IOException
	 */
	@Override
	public void writeCentiCell(int cellId, int centiCellId, int count) throws IOException{
		LatLongBoundingBox llbb = CellIdUtils.toBoundingBox(cellId, centiCellId);
		writeOutLatLongBoundingBox(cellId,centiCellId,-1,llbb,count);
	}
	
	/**
	 * Output a tenmilli cell density
	 * 
	 * @param cellId
	 * @param tenMilliCellId
	 * @param count
	 * @throws IOException
	 */
	@Override
	public void writeTenMilliCell(int cellId, int tenMilliCellId, int count) throws IOException{
		LatLongBoundingBox llbb = tenMilliToBoundingBox(cellId, tenMilliCellId);
		writeOutLatLongBoundingBox(cellId, -1, tenMilliCellId, llbb, count);
	}

	/**
	 * @return the hostUrl
	 */
	public String getHostUrl() {
		return hostUrl;
	}

	/**
	 * @param hostUrl the hostUrl to set
	 */
	public void setHostUrl(String hostUrl) {
		this.hostUrl = hostUrl;
	}


}
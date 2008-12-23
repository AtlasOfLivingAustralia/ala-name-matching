package org.ala.dao.geo;

import java.io.IOException;

import org.ala.io.CellDensityOutputStream;

/**
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public interface CellDensityDAO {

	public void outputCellDensities(long entityId, int type, int minCellId, int maxCellId,
			CellDensityOutputStream output)
		throws IOException;
	
	public void outputCellDensities(long entityId, int type, CellDensityOutputStream output)
	throws IOException;
	
	
	public void outputCentiCellDensities(long entityId, int type, CellDensityOutputStream output)
		throws IOException;

	public void outputTenMilliCellDensities(long entityId, int type, CellDensityOutputStream output)
		throws IOException;
	
	public void outputCentiCellDensities(long entityId, int type,
			int minCellId, int maxCellId, CellDensityOutputStream output)
			throws IOException;
}

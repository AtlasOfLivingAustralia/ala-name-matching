package org.ala.dao.geo.jdbc.impl;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.ala.dao.geo.CellDensityDAO;
import org.ala.io.CellDensityOutputStream;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * Cell Density DAO Implementation using Springs Jdbc Support for performance.
 * 
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public class CellDensityDAOImpl extends JdbcDaoSupport implements CellDensityDAO{

	/**
	 * @see org.ala.dao.geo.CellDensityDAO#outputCellDensities(long, long, org.gbif.portal.io.ResultsOutputStream)
	 */
	@SuppressWarnings("unchecked")
	public void outputCellDensities(long entityId, int type,
			CellDensityOutputStream cdos) throws IOException {
		List<Map<String, Object>> densities = getJdbcTemplate().queryForList(
				"select cell_id, count from cell_density " +
				"where entity_id=? and type=?", new Object[]{entityId, type});
		for(Map<String, Object> density: densities){
			cdos.writeCell( ((Number)(density.get("cell_id"))).intValue(), 
					((Number)(density.get("count"))).intValue());
		}
	}

	/**
	 * @see org.ala.dao.geo.CellDensityDAO#outputCellDensities(long, int, int, int, org.ala.io.CellDensityOutputStream)
	 */
	public void outputCellDensities(long entityId, int type,
			int minCellId, int maxCellId, CellDensityOutputStream cdos)
			throws IOException {
		List<Map<String, Object>> densities = getJdbcTemplate().queryForList(
				"select cell_id, count from centi_cell_density " +
				"where entity_id=? and type=? " +
				"and mod(cell_id,360)>=? and cell_id>=? " +
				"and mod(cell_id,360)<=? and cell_id<=?",
				new Object[]{entityId, type, minCellId % 360, minCellId, maxCellId % 360, maxCellId});
		for(Map density: densities)
			cdos.writeCell(((Number)(density.get("cell_id"))).intValue(), 
					((Number)(density.get("count"))).intValue());
	}	
	
	/**
	 * @see org.ala.dao.geo.CellDensityDAO#outputCentiCellDensities(long, long, org.ala.io.ResultsOutputStream)
	 */
	public void outputCentiCellDensities(final long entityId, final int type,
			final CellDensityOutputStream cdos) throws IOException {
		getJdbcTemplate().query("select cell_id, centi_cell_id, count from centi_cell_density where entity_id=? and type=?", 
				new PreparedStatementSetter(){
					public void setValues(PreparedStatement pstmt)
							throws SQLException {
						pstmt.setLong(1, entityId);
						pstmt.setInt(2, type);
					}
				},
				new ResultSetExtractor(){
					public Object extractData(ResultSet rs) throws SQLException,
							DataAccessException {
						while(rs.next()){
							int cellId = rs.getInt("cell_id");
							int centiCellId = rs.getInt("centi_cell_id");
							int count = rs.getInt("count");
							try{
								cdos.writeCentiCell(cellId,centiCellId,count);
							} catch(IOException e){
								throw new SQLException(e.getMessage());
							}
						}
						return null;
					}
				}
		);
	}

	/**
	 * @see org.ala.dao.geo.CellDensityDAO#outputCentiCellDensities(long, int, int, int, org.ala.io.CellDensityOutputStream)
	 */
	public void outputCentiCellDensities(final long entityId, final int type,
			final int minCellId, final int maxCellId, final CellDensityOutputStream cdos)
			throws IOException {
		getJdbcTemplate().query("select cell_id, centi_cell_id, count from centi_cell_density " +
			"where entity_id=? and type=? " +
			"and mod(cell_id,360)>=? and cell_id>=? " +
			"and mod(cell_id,360)<=? and cell_id<=?", 
			new PreparedStatementSetter(){
				public void setValues(PreparedStatement pstmt)
						throws SQLException {
					pstmt.setLong(1, entityId);
					pstmt.setInt(2, type);
					pstmt.setInt(3, minCellId % 360);
					pstmt.setInt(4, minCellId);
					pstmt.setInt(5, maxCellId % 360);
					pstmt.setInt(6, maxCellId);
				}
			},
			new ResultSetExtractor(){
				public Object extractData(ResultSet rs) throws SQLException,
						DataAccessException {
					while(rs.next()){
						int cellId = rs.getInt("cell_id");
						int centiCellId = rs.getInt("centi_cell_id");
						int count = rs.getInt("count");
						try{
							cdos.writeCentiCell(cellId,centiCellId,count);
						} catch(IOException e){
							throw new SQLException(e.getMessage());
						}
					}
					return null;
				}
			}
		);
	}

	/**
	 * @see org.ala.dao.geo.CellDensityDAO#outputTenMilliCellDensities(long, long, org.ala.io.ResultsOutputStream)
	 */
	public void outputTenMilliCellDensities(long entityId, int type,
			CellDensityOutputStream cdos) throws IOException {
		List<Map<String, Object>> densities = getJdbcTemplate().queryForList(
				"select cell_id, tenmilli_cell_id, count from tenmilli_cell_density " +
				"where entity_id=? and type=?", new Object[]{entityId, type});
		for(Map density: densities)
			cdos.writeTenMilliCell(
					((Number)(density.get("cell_id"))).intValue(), 
					((Number)(density.get("tenmilli_cell_id"))).intValue(),
					((Number)(density.get("count"))).intValue());
	}
	
	/**
	 * @see org.ala.dao.geo.CellDensityDAO#outputTenMilliCellDensities(long, int, int, int, org.ala.io.CellDensityOutputStream)
	 */
	public void outputTenMilliCellDensities(final long entityId, final int type,
			final int minCellId, final int maxCellId, final CellDensityOutputStream cdos)
			throws IOException {
		
		getJdbcTemplate().query("select cell_id, tenmilli_cell_id, count from tenmilli_cell_density " +
			"where entity_id=? and type=? " +
			"and mod(cell_id,360)>=? and cell_id>=? " +
			"and mod(cell_id,360)<=? and cell_id<=?", 
			new PreparedStatementSetter(){
				public void setValues(PreparedStatement pstmt)
						throws SQLException {
					pstmt.setLong(1, entityId);
					pstmt.setInt(2, type);
					pstmt.setInt(3, minCellId % 360);
					pstmt.setInt(4, minCellId);
					pstmt.setInt(5, maxCellId % 360);
					pstmt.setInt(6, maxCellId);
				}
			},
			new ResultSetExtractor(){
				public Object extractData(ResultSet rs) throws SQLException,
						DataAccessException {
					while(rs.next()){
						int cellId = rs.getInt("cell_id");
						int tenmilliCellId = rs.getInt("tenmilli_cell_id");
						int count = rs.getInt("count");
						try{
							cdos.writeTenMilliCell(cellId,tenmilliCellId,count);
						} catch(IOException e){
							throw new SQLException(e.getMessage());
						}
					}
					return null;
				}
			});
	}
}
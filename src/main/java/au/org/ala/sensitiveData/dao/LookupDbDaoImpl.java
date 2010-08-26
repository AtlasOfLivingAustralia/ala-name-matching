package au.org.ala.sensitiveData.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import au.org.ala.sensitiveData.model.SensitiveSpecies;
import au.org.ala.sensitiveData.model.SensitivityCategory;

public class LookupDbDaoImpl extends JdbcDaoSupport implements LookupDao {
	
	protected void initDao() throws Exception {
		super.initDao();
	}
	
	public SensitiveSpecies findByName(String scientificName) {
		return getJdbcTemplate().queryForObject(
				"SELECT * FROM sensitive-species WHERE scientific_name = ?",
				new Object[] { scientificName },
				new RowMapper<SensitiveSpecies>() {
					public SensitiveSpecies mapRow(ResultSet rs, int row) throws SQLException {
						SensitiveSpecies ss = new SensitiveSpecies(rs.getString("scientific_name"), SensitivityCategory.getCategory(rs.getInt("category")));
						return ss;
					}
				});
	}

}

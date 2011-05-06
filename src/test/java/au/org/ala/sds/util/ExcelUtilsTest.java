package au.org.ala.sds.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class ExcelUtilsTest {

    @Test
    public void ordinalTest() {
        assertEquals(1, ExcelUtils.getOrdinal("A"));
        assertEquals(26, ExcelUtils.getOrdinal("Z"));
        assertEquals(27, ExcelUtils.getOrdinal("AA"));
        assertEquals(53, ExcelUtils.getOrdinal("BA"));
        assertEquals(702, ExcelUtils.getOrdinal("ZZ"));
        assertEquals(703, ExcelUtils.getOrdinal("AAA"));
    }
}

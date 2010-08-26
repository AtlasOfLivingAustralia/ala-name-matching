package au.org.ala.sensitiveData;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import au.org.ala.sensitiveData.model.SensitiveSpecies;

import junit.framework.TestCase;

public class GeneraliseTest extends TestCase {

	ApplicationContext context;
	SearchImpl finder;
	
	protected void setUp() throws Exception {
		super.setUp();
		context = new ClassPathXmlApplicationContext("spring-config.xml");
		finder = context.getBean("searchImpl", SearchImpl.class);
	}

	public void testGeneralisation() {
		SensitiveSpecies ss = finder.findSensitiveSpecies("Crex crex");
		assertNotNull(ss);
		
		String latitude = "-67.2883";
		String longitude = "71.1993";
		String[] coords = finder.generaliseLocation(ss, latitude, longitude);
		assertEquals("Latitude", "-67.29", coords[0]);
		assertEquals("Longitude", "71.20", coords[1]);

		ss = finder.findSensitiveSpecies("Dasyornis brachypterus");
		assertNotNull(ss);
		
		latitude = "-67.2883";
		longitude = "71.1993";
		coords = finder.generaliseLocation(ss, latitude, longitude);
		assertEquals("Latitude", "-67.3", coords[0]);
		assertEquals("Longitude", "71.2", coords[1]);
	}
}

package au.org.ala.sensitiveData;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import au.org.ala.sensitiveData.model.SensitiveSpecies;

import junit.framework.TestCase;

public class SearchTest extends TestCase {

	ApplicationContext context;
	SearchImpl finder;
	
	protected void setUp() throws Exception {
		super.setUp();
		context = new ClassPathXmlApplicationContext("spring-config.xml");
		finder = context.getBean("searchImpl", SearchImpl.class);
	}

	public void testLookup() {
		SensitiveSpecies ss = finder.findSensitiveSpecies("Macropus rufus");
		assertNull(ss);

		ss = finder.findSensitiveSpecies("Crex crex");
		assertNotNull(ss);
	}
}

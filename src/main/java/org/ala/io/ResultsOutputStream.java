/**
 * 
 */
package org.ala.io;

import java.io.IOException;
import java.util.Map;

/**
 * 
 *
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public interface ResultsOutputStream {

	public void write(Map<String, Object> mapToOutput, String[] fieldsInOrder) throws IOException;
}

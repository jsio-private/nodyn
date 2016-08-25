package io.nodyn.runtime.j2v8;

import com.eclipsesource.v8.V8;
import io.nodyn.runtime.Program;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author emir
 */
public class J2V8Program implements Program {

	private final String source;

	public J2V8Program(String source) {
		this.source = source;
	}
	
	public J2V8Program(String source, String fileName) {
		this.source = source;
	}

	@Override
	public Object execute(Object context) {
		try {
			return ((V8) context).executeScript(source);
		} catch (Exception ex) {
			Logger.getLogger(J2V8Program.class.getName()).log(Level.SEVERE, null, ex);
		}
		throw new RuntimeException("Failed to run script (J2V8)");
	}

}

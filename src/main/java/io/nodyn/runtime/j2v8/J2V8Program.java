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

	private final String fileName;
	private final String source;

	public J2V8Program(String source) {
		this.source = source;
		this.fileName = null;
	}
	
	public J2V8Program(String source, String fileName) {
		this.fileName = fileName;
		this.source = source;
	}

	@Override
	public Object execute(Object context) {
		try {
			System.out.println("exec script");
			Object ret = ((V8) context).executeScript(source, fileName, 0);
			System.out.println("ret : "+ret.getClass().getName());
			return ret;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		throw new RuntimeException("Failed to run script (J2V8)");
	}

}

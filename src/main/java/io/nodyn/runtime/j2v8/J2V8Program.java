package io.nodyn.runtime.j2v8;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import io.nodyn.runtime.Program;

/**
 *
 * @author emir
 */
public class J2V8Program implements Program {

	private final V8 v8;
	private final String fileName;
	private final String source;

	public J2V8Program(V8 v8, String source) {
		this.v8 = v8;
		this.source = source;
		this.fileName = null;
	}
	
	public J2V8Program(V8 v8, String source, String fileName) {
		this.v8 = v8;
		this.fileName = fileName;
		this.source = source;
	}

	@Override
	public Object execute(Object contextObject) {
		try {
			if (contextObject instanceof V8Object) {
				V8Object context = (V8Object) contextObject;
				if (!context.isUndefined()) {
					context.registerJavaMethod(new JavaCallback() {

						@Override
						public Object invoke(V8Object vo, V8Array va) {
							return v8.executeScript(source, fileName, 0);
						}
						
					}, "tmpfnc");
					return context.executeFunction("tmpfnc", new V8Array(v8));
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		throw new RuntimeException("Failed to run script (J2V8)");
	}

}

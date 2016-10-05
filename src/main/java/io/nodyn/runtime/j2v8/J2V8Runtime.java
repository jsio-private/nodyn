package io.nodyn.runtime.j2v8;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import io.js.J2V8Classes.Utils;
import io.nodyn.NodeProcess;
import io.nodyn.Nodyn;
import io.nodyn.runtime.NodynConfig;
import io.nodyn.runtime.Program;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.vertx.java.core.Vertx;

import javax.script.ScriptException;
import org.vertx.java.core.VertxFactory;

/**
 *
 * @author emir
 */
public class J2V8Runtime extends Nodyn {

	private final io.js.J2V8Classes.Runtime engine;
//    private final ScriptContext global;
	private Program nativeRequire;
	private Object context;

	private static final String NATIVE_REQUIRE = "nodyn/_native_require.js";

	public J2V8Runtime(NodynConfig config) {
		this(config, VertxFactory.newVertx(), true);
	}

	public J2V8Runtime(NodynConfig config, Vertx vertx, boolean controlLifeCycle) {
		super(config, vertx, controlLifeCycle);
		Thread.currentThread().setContextClassLoader(getConfiguration().getClassLoader());
		engine = new io.js.J2V8Classes.Runtime("nodyn");
		context = engine.getRuntime();
		
		Logger.getLogger("Runtime-nodyn").setLevel(Level.OFF);
		Logger.getLogger("ClassGenerator").setLevel(Level.OFF);
		Logger.getLogger("Utils").setLevel(Level.OFF);

		try {
			nativeRequire = compileNative(NATIVE_REQUIRE);
			nativeRequire.execute(engine.getRuntime());
		} catch (Exception ex) {
			Logger.getLogger(J2V8Runtime.class.getName()).log(Level.SEVERE, "Failed to load " + NATIVE_REQUIRE, ex);
			System.exit(255);
		}
	}

	@Override
	public Object loadBinding(String name) {
		try {
			String pathName = "nodyn/bindings/" + name + ".js";
			Object ret = engine.getRuntime().executeScript("_native_require('" + pathName + "');");
			return ret;
		} catch (Exception e) {
			this.handleThrowable(e);
		}
		return false;
	}

	@Override
	public Program compile(String source, String fileName, boolean displayErrors) throws Throwable {
		// TODO: do something with the displayErrors parameter
		try {
			Program program = new J2V8Program(engine.getRuntime(), source, fileName);
			return program;
		} catch (Exception ex) {
			Logger.getLogger(J2V8Runtime.class.getName()).log(Level.SEVERE, "Cannot compile script " + fileName, ex);
			handleThrowable(ex);
		}
		return null;
	}

	@Override
	public void makeContext(Object init) {
		this.context = init;
		System.out.println("make context to " + init);
		System.exit(-1);
	}

	@Override
	public boolean isContext(Object ctx) {
		return ctx == context;
	}

	@Override
	public void handleThrowable(Throwable t) {
		System.err.println(t);
		t.printStackTrace();
		System.exit(-1);
	}

	@Override
	protected NodeProcess initialize() {
		V8 v8 = engine.getRuntime();

		NodeProcess javaProcess = new NodeProcess(this);
		getEventLoop().setProcess(javaProcess);
		
		V8Object jsProcess = new V8Object(v8);
		
		jsProcess.add("__javaClass", javaProcess.getClass().getName());
		jsProcess.add("__javaInstance", Utils.registerInstance(javaProcess));

		v8.add("__vertx", Utils.getV8ObjectForObject(v8, getVertx()));
		v8.add("__dirname", System.getProperty("user.dir"));
		v8.add("__filename", Nodyn.NODE_JS);
		v8.add("__nodyn", Utils.getV8ObjectForObject(v8, this));
		
		// there's probably a better way to do this...
		v8.add("tmp", jsProcess);
		v8.executeScript("ClassHelpers.addJavaFieldsToObj(tmp)");
		v8.executeScript("ClassHelpers.addJavaFieldsToObj(__nodyn)");
		v8.executeScript("ClassHelpers.addJavaFieldsToObj(__vertx)");

		try {
			v8.executeScript("global = this;");
			// Adds ES6 capabilities not provided by DynJS to global scope
			compileNative(ES6_POLYFILL).execute(v8);

			// Invoke the process function
			V8Function processFunction = (V8Function) compileNative(PROCESS).execute(v8);
			V8Function nodeFunction = (V8Function) compileNative(NODE_JS).execute(v8);
			
			V8Object returnedProcess = (V8Object) processFunction.call(v8, new V8Array(v8).push(jsProcess));
			nodeFunction.call(v8, new V8Array(v8).push(returnedProcess));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return javaProcess;
	}

	@Override
	protected Object runScript(String script) {
		try {
			String fr = Files.toString(new File(script), Charset.defaultCharset());
			return engine.getRuntime().executeScript(fr);
		} catch (Exception ex) {
			Logger.getLogger(J2V8Runtime.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	@Override
	public Object getGlobalContext() {
		return context;
	}

	private Program compileNative(String fileName) throws ScriptException {
		try {
			final InputStreamReader is = new InputStreamReader(getConfiguration().getClassLoader().getResourceAsStream(fileName));
			String source = CharStreams.toString(is);
			return new J2V8Program(engine.getRuntime(), source, fileName);
		} catch (IOException ex) {
			throw new ScriptException(ex);
		}
	}
}

package io.nodyn.runtime.j2v8;

import com.eclipsesource.v8.V8;
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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.vertx.java.core.Vertx;

import javax.script.ScriptContext;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.AbstractJSObject;
import jdk.nashorn.api.scripting.JSObject;
import org.vertx.java.core.VertxFactory;


/**
 *
 * @author emir
 */
public class J2V8Runtime extends Nodyn {

    private final io.js.J2V8Classes.Runtime engine;
//    private final ScriptContext global;
    private Program nativeRequire;
    
    private static final String NATIVE_REQUIRE = "nodyn/_native_require.js";

    public J2V8Runtime(NodynConfig config) {
        this(config, VertxFactory.newVertx(), true);
    }


    public J2V8Runtime(NodynConfig config, Vertx vertx, boolean controlLifeCycle) {
        super(config, vertx, controlLifeCycle);
        Thread.currentThread().setContextClassLoader(getConfiguration().getClassLoader());
        engine = new io.js.J2V8Classes.Runtime("nodyn");

        try {
            nativeRequire = compileNative(NATIVE_REQUIRE);
            nativeRequire.execute(engine.getRuntime());
			
			System.out.println("done executing nativeRequire");
        } catch (Exception ex) {
            Logger.getLogger(J2V8Runtime.class.getName()).log(Level.SEVERE, "Failed to load " + NATIVE_REQUIRE, ex);
            System.exit(255);
        }
    }

    @Override
    public Object loadBinding(String name) {
        try {
            String pathName = "nodyn/bindings/" + name + ".js";
			
			System.out.println("loading binding..");
            return engine.getRuntime().executeScript("_native_require('" + pathName + "');");
        } catch (Exception e) {
            this.handleThrowable(e);
        }
        return false;
    }

    @Override
    public Program compile(String source, String fileName, boolean displayErrors) throws Throwable {
        // TODO: do something with the displayErrors parameter
        try {
            Program program = new J2V8Program(source, fileName);
            return program;
        } catch (Exception ex) {
            Logger.getLogger(J2V8Runtime.class.getName()).log(Level.SEVERE, "Cannot compile script " + fileName, ex);
            handleThrowable(ex);
        }
        return null;
    }

    @Override
    public void makeContext(Object init) {
    }

    @Override
    public boolean isContext(Object ctx) {
        return false;
    }

    @Override
    public void handleThrowable(Throwable t) {
        System.err.println(t);
        t.printStackTrace();
    }

    @Override
    protected NodeProcess initialize() {
        V8 v8 = engine.getRuntime();
        v8.add("__vertx", Utils.getV8ObjectForObject(v8, getVertx()));
        v8.add("__dirname", System.getProperty("user.dir"));
        v8.add("__filename", Nodyn.NODE_JS);
        v8.add("__nodyn", Utils.getV8ObjectForObject(v8, this));
        
        NodeProcess javaProcess = new NodeProcess(this);
        getEventLoop().setProcess(javaProcess);

        try {
			v8.executeScript("global = this;");
            v8.executeScript("load(\"nashorn:mozilla_compat.js\");");

            // Adds ES6 capabilities not provided by DynJS to global scope
            compileNative(ES6_POLYFILL).execute(v8);

            // Invoke the process function
            JSObject processFunction = (JSObject) compileNative(PROCESS).execute(v8);
            JSObject jsProcess = (JSObject) processFunction.call(processFunction, javaProcess);

            // Invoke the node function
            JSObject nodeFunction = (JSObject) compileNative(NODE_JS).execute(v8);
			System.out.println("calling node function");
            nodeFunction.call(nodeFunction, jsProcess);
			System.out.println("done node func");
        } catch (ScriptException ex) {
            Logger.getLogger(J2V8Runtime.class.getName()).log(Level.SEVERE, "Cannot initialize", ex);
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
        return engine.getRuntime();
    }
    
    private Program compileNative(String fileName) throws ScriptException  {
		try {
			final InputStreamReader is = new InputStreamReader(getConfiguration().getClassLoader().getResourceAsStream(fileName));
			String source = CharStreams.toString(is);
			return new J2V8Program(source, fileName);
		} catch (IOException ex) {
			throw new ScriptException(ex);
		}
    }

    class NodynJSObject extends AbstractJSObject {

        HashMap store = new HashMap();

        @Override
        public void setMember(String name, Object value) {
            store.put(name, value);
        }

        @Override
        public boolean hasMember(String name) {
            return store.containsKey(name);
        }

        @Override
        public Object getMember(String name) {
            return store.get(name);
        }
    }
}

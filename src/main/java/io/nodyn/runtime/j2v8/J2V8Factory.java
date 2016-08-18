package io.nodyn.runtime.j2v8;

import io.nodyn.Nodyn;
import io.nodyn.runtime.NodynConfig;
import io.nodyn.runtime.RuntimeFactory;
import org.vertx.java.core.Vertx;

/**
 *
 * @author Emir
 */
public class J2V8Factory extends RuntimeFactory {

    public J2V8Factory(ClassLoader parent) {
        super(parent);
    }

    @Override
    public Nodyn newRuntime(NodynConfig config) {
        return new J2V8Runtime(config);
    }

    @Override
    public Nodyn newRuntime(NodynConfig config, Vertx vertx) {
        return new J2V8Runtime(config, vertx, true);
    }
}

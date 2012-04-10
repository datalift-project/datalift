package org.datalift.tests;


import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Value;

import static org.datalift.fwk.util.StringUtils.*;


abstract class SparqlFunction implements SparqlExpression
{
    private final static Map<String,Class<? extends SparqlFunction>> fcts =
                        new HashMap<String,Class<? extends SparqlFunction>>();

    public final String name;
    protected final List<Value> args;

    static {
        register(new Concat());
    }

    protected SparqlFunction(String name, Value... v) {
        this.name = name;
        List<Value> l = Collections.emptyList();
        if (v != null) {
            l = Arrays.asList(v);
        }
        this.args = l;
    }

    @Override
    public String stringValue() {
        return this.name + '(' + join(this.args, ", ") + ')';
    }

    @Override
    public final String toString() {
        return this.stringValue();
    }

    public static void register(SparqlFunction f) {
        fcts.put(f.name, f.getClass());
    }

    public static SparqlFunction newFunction(String name, Value... v) {
        SparqlFunction f = null;

        Class<? extends SparqlFunction> c = fcts.get(name);
        if (c != null) {
            try {
                Constructor<? extends SparqlFunction> ctor =
                                c.getConstructor(new Class[] { Value[].class });
                f = ctor.newInstance(new Object[] { v });
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return f;
    }

    private final static class Concat extends SparqlFunction
    {
        public Concat() {
            this((Value[])null);
        }

        public Concat(Value... v) {
            super("concat", v);
        }
    }
}

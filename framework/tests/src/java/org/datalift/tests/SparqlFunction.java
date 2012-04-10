/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.tests;


import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;

import static org.datalift.fwk.util.StringUtils.*;


abstract class SparqlFunction implements SparqlExpression
{
    private final static Map<String,Class<? extends SparqlFunction>> fcts =
                        new HashMap<String,Class<? extends SparqlFunction>>();

    public final String name;
    protected final List<Value> args;

    static {
        register(new Expr());
        register(new Concat());
        register(new Strlen());
        register(new Substr());
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
        fcts.put(f.name.toLowerCase(), f.getClass());
    }

    public static SparqlFunction newFunction(String name, Value... v) {
        SparqlFunction f = null;

        Class<? extends SparqlFunction> c = fcts.get(name.toLowerCase());
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
        private Concat() {
            super("concat", (Value[])null);
        }

        public Concat(Value... args) {
            super("concat", args);
            if (args.length < 2) {
                throw new IllegalArgumentException(
                    "CONCAT(string literal ltrl1 ... string literal ltrlN");
            }
        }
    }

    private final static class Expr extends SparqlFunction
    {
        private Expr() {
            super("expr", (Value[])null);
        }

        public Expr(Value... args) {
            super("expr", args);
            if (args.length != 1) {
                throw new IllegalArgumentException("EXPR(string literal expr)");
            }
        }
    }

    private final static class Strlen extends SparqlFunction
    {
        private Strlen() {
            super("strlen", (Value[])null);
        }

        public Strlen(Value... args) {
            super("strlen", args);
            if (args.length != 1) {
                throw new IllegalArgumentException("STRLEN(string literal str)");
            }
        }
    }

    private final static class Substr extends SparqlFunction
    {
        private Substr() {
            super("substr", (Value[])null);
        }

        public Substr(Value... args) {
            super("substr", args);
            boolean valid = ((args.length >= 2) && (args.length <= 3));
            if (valid) {
                try {
                    valid = ((args[1] instanceof Variable) ||
                             (((Literal)args[1]).intValue() >= 0));
                    if (valid && (args.length > 2)) {
                        valid = ((args[2] instanceof Variable) ||
                                 (((Literal)args[2]).intValue() >= 0));
                    }
                }
                catch (Exception e) {
                    valid = false;
                }
            }
            if (! valid) {
                throw new IllegalArgumentException(
                    "SUBSTR(string literal source, xsd:integer startingLoc " +
                    "[, xsd:integer length])");
            }
        }
    }
}

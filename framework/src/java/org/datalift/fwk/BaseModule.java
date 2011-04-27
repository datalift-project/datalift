package org.datalift.fwk;


import java.util.Map;


/**
 * A default implementation of the {@link Module} interface to act
 * as a superclass for actual application modules.
 *
 * @author lbihanic
 */
public abstract class BaseModule implements Module
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The module name. */
    private final String name;
    /** Whether the module acts as a JAX_RS resource. */
    private final boolean isResource;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new module.
     * <p>
     * This constructor is a shortcut to
     * <code>new BaseModule(name, false)</code>.
     * @param  name   the module name.
     *
     * @throws IllegalArgumentException if <code>name</code> is
     *         <code>null</code>.
     */
    protected BaseModule(String name) {
        this(name, false);
    }

    /**
     * Creates a new module.
     * @param  name         the module name.
     * @param  isResource   whether this module acts as a JAX-RS
     *                      resource.
     *
     * @throws IllegalArgumentException if <code>name</code> is
     *         <code>null</code>.
     */
    protected BaseModule(String name, boolean isResource) {
        if ((name == null) || (name.length() == 0)) {
            throw new IllegalArgumentException("name");
        }
        this.name = name;
        this.isResource = isResource;
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>This implementation returns the module name provided as
     * constructor argument.</p>
     */
    @Override
    public final String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * <p>This implementation returns the <code>isResource</code>
     * flag provided as constructor argument.</p>
     */
    @Override
    public final boolean isResource() {
        return this.isResource;
    }

    /**
     * {@inheritDoc}
     * <p>This implementation is empty.</p>
     */
    @Override
    public void init(Configuration cfg) {
        // NOP
    }

    @Override
    /**
     * {@inheritDoc}
     * <p>This implementation is empty.</p>
     */
    public void shutdown(Configuration cfg) {
        // NOP
    }

    /**
     * {@inheritDoc}
     * <p>This implementation returns <code>null</code> (no resources).</p>
     */
    @Override
    public Map<String,Class<?>> getResources() {
        return null;
    }
}

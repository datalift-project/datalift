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

package org.datalift.fwk;


import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;


/**
 * <code>TechnicalExceptions</code> are thrown to indicate technical
 * or environmental (i.e. configuration, hardware...) error conditions
 * that need not to be handled by the application code. These errors
 * shall be notified to the system administrator.
 *
 * @author  lbihanic
 */
public abstract class TechnicalException extends RuntimeException
{
    //------------------------------------------------------------------------
    // Constants
    //------------------------------------------------------------------------

    protected final static String DEFAULT_BUNDLE_NAME = "/error-messages";

    //------------------------------------------------------------------------
    // Instance members definition
    //------------------------------------------------------------------------

    /**
     * The message code, i.e. the name of the message format in the
     * resource bundle or <code>null</code> if the message is
     * specified as a string directly containing the message format
     * or text.
     */
    private String messageCode = null;
    /** The message arguments, if any. */
    private transient Object[] messageArgs = null;

    //------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------

    /**
     * Constructs a new exception without any detail message.
     */
    protected TechnicalException() {
        super();
    }

    /**
     * Constructs a new exception with the specified cause but no
     * detail message.  The detail message of this exception will
     * be the detail message of the cause.
     * @param  cause   the cause.
     */
    protected TechnicalException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified message code
     * and arguments to build a detail message from the format
     * associated to the message code.
     * <p>
     * The message code can be either the actual message format or
     * the identifier of a resource (defined in the
     * {@link #getMessageBundleName exception type resource bundle})
     * that contains the message format.</p>
     * @param  code   the message code or format. In the latter
     *                case, it shall be compliant with the
     *                grammar defined by {@link MessageFormat}.
     * @param  data   the arguments to build the detail message
     *                from the format.
     */
    protected TechnicalException(String code, Object... data) {
        this(code, null, data);
    }

    /**
     * Constructs a new exception with the specified message code
     * and the arguments to build a detail message from the format
     * associated to the message code.
     * <p>
     * The message code can be either the actual message format or
     * the identifier of a resource (defined in the
     * {@link #getMessageBundleName exception type resource bundle})
     * that contains the message format.</p>
     * <p>
     * Note that the detail message associated with
     * <code>cause</code> is <em>not</em> automatically incorporated
     * in this exception's detail message.</p>
     * @param  code    the message code or format. In the latter
     *                 case, it shall be compliant with the
     *                 grammar defined by {@link MessageFormat}.
     * @param  cause   the cause. A <code>null</code> value is
     *                 allowed to indicate that the cause is
     *                 nonexistent or unknown.
     * @param  data    the arguments to build the detail message
     *                 from the format.
     */
    protected TechnicalException(String code, Throwable cause, Object... data) {
        super(code, cause);
        this.messageCode = code;
        this.messageArgs = data;
    }

    //------------------------------------------------------------------------
    // Exception interface support
    //------------------------------------------------------------------------

    /**
     * Returns the detail message for this exception, formatting
     * it from the message code and arguments if need be.
     * <p>
     * This default implementation invokes
     * {@link #getMessage(java.util.Locale)} with the default system
     * locale.</p>
     * <p>
     * Subclasses requiring specific user locale support shall
     * overwrite this method and consider overwriting
     * {@link #getLocalizedMessage()} as well.</p>
     *
     * @return the detail message.
     *
     * @see    #getLocalizedMessage()
     */
    public String getMessage() {
        return this.getMessage(Locale.getDefault());
    }

    //------------------------------------------------------------------------
    // Specific implementation
    //------------------------------------------------------------------------

    /**
     * Returns the message code.
     *
     * @return the message code.
     */
    public String getMessageCode() {
        return this.messageCode;
    }

    /**
     * Returns the name of the message bundle to use for formatting
     * error messages for this exception type.  The returned name is
     * relative to the classpath.
     * <p>
     * Subclasses should overwrite this method to enforce their own
     * naming convention if need be.</p>
     * <p>
     * This default implementation returns a bundle named
     * "<code>error-messages</code>" located in the same package as
     * the exception class.</p>
     *
     * @return the path to the message bundle for this exception or
     *         <code>null</code> if no message formatting shall be
     *         attempted.
     */
    protected String getMessageBundleName() {
        return this.getClass().getPackage().getName().replace('.', '/')
                                                        + DEFAULT_BUNDLE_NAME;
    }

    /**
     * Formats the exception detail message.
     *
     * @param  locale   the locale for which the message shall be
     *                  formatted.  If no locale is specified, the
     *                  default system one will be used.
     *
     * @return a formatted detail message.
     */
    protected String getMessage(Locale locale) {
        String message = null;

        if (this.messageCode != null) {
            Object[] args = this.messageArgs;

            String format = this.getMessageFormat(this.messageCode, locale);
            if (format != null) {
                if (args != null) {
                    // Arguments are provided. => Format message.
                    try {
                        message = MessageFormat.format(format, args);
                    }
                    catch (Exception e) { /* Ignore... */ }
                }
                else {
                    // No arguments. => use the format string as message.
                    message = format;
                }
            }
            if (message == null) {
                if ((args != null) && (args.length != 0)) {
                    StringBuilder buf = new StringBuilder(this.messageCode);

                    // No format found for this message identifier
                    // or message formatting error encountered.
                    // => Just dump the message identifier and each of the
                    //    arguments as strings.
                    for (Object arg : args) {
                        buf.append(" \"").append(String.valueOf(arg))
                           .append('\"');
                    }
                    message = buf.toString();
                }
                else {
                    message = this.messageCode;
                }
            }
        }
        else {
            message = super.getMessage();
        }
        return message;
    }

    /**
     * Returns the format associated to the specified key.
     *
     * @param  key      the name of the message format to retrieve.
     * @param  locale   the locale for which the message format shall
     *                  be retrieved.
     *
     * @return the message format associated to the key, the key
     *         itself (if no corresponding resource was found) or
     *         <code>null</code> (if the
     *         {@link #getMessageBundleName resource bundle} can not
     *         be loaded).
     */
    private final String getMessageFormat(String key, Locale locale) {
        String format = key;
        String bundleName = this.getMessageBundleName();

        if ((key != null) && (bundleName != null)) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            try {
                ResourceBundle bundle = ResourceBundle.getBundle(
                                                        bundleName, locale);
                try {
                    format = bundle.getString(key);
                }
                catch (MissingResourceException e) { /* Ignore... */ }
            }
            catch (MissingResourceException e) {
                e.printStackTrace();
                format = null;
            }
        }
        // Else: No message key or message formatting not supported.

        return format;
    }
}


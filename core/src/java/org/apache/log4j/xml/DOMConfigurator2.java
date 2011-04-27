package org.apache.log4j.xml;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.config.PropertySetter;
import org.apache.log4j.helpers.Loader;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.ThrowableRenderer;
import org.apache.log4j.spi.ThrowableRendererSupport;


/**
 * Enhanced {@link DOMConfigurator}. This implementation adds:
 * <ul>
 *   <li>XML Namespace support</li>
 *   <li>Schema-based (optional) XML validation of Log4J
 *   configurations</li>
 *   <li>Log4J configurations spanning multiple files through the
 *   use of XInclude (Java 5+ required) with possibility to provide a
 *   fallback configuration in case the to-be-included file is not
 *   found</li>
 *   <li>Log4J configuration embedded within any XML data; no
 *   requirement for Log4J dedicated configuration files any longer</li>
 *   <li>Support for <code>classpath:</code> URI scheme for external
 *   XML entities, allowing JAR/WAR/EAR embedded Log4J configuration
 *   to include files anywhere in the JVM classpath (other JARs,
 *   file system directories...)</li>
 *   <li>Scoped variables defined in the XML configuration</li>
 *   <li>Automatic check for configuration updates, including on
 *   included files, with detection on newly added or removed
 *   included files.</li>
 * </ul>
 * <p>
 * To use this configurator, set the system property
 * <code>log4j.configuratorClass</code> to
 * <code>org.apache.log4j.xml.DOMConfigurator2</code>, for example
 * by adding the following option to the Java VM command line:
 * <code>-Dlog4j.configuratorClass=org.apache.log4j.xml.DOMConfigurator2</code></p>
 * <p>
 * To activate XML validation of the Log4J configuration, set the
 * system property <code>log4j.validateConfiguration</code> to
 * <code>true</code>.</p>
 * <p>
 * Due to full XML Namespace support, previous log4j.xml configuration
 * files are not supported. All configuration items
 * (<code>&lt;appender&gt;</code>, <code>&lt;logger&gt;</code>,
 * <code>&lt;root&gt;</code>, ...) shall now belong to the same namespace
 * as the root configuration tag (<code>&lt;configuration&gt;</code>).</p>
 * <p>
 * To make your existing log4j.xml compatible, you can either:</p>
 * <ul>
 *  <li>Double-declare the Log4J namespace, with prefix (for the
 *  root element) and as default namespace (for the subsequent
 *  configuration items):
 * <blockquote><pre>
 *     &lt;log4j:configuration xmlns="http://jakarta.apache.org/log4j/"
 *             xmlns:log4j="http://jakarta.apache.org/log4j/"&gt;
 * </pre></blockquote>
 *  This is the only option to keep your Log4J XML configuration
 *  compatible with the previous DOMConfigurator.</li>
 *  <li>Make the Log4J namespace the default namespace by removing the
 *  <code>log4j</code> prefix:
 * <blockquote><pre>
 *     &lt;configuration xmlns="http://jakarta.apache.org/log4j/"&gt;
 *     &lt;/configuration&gt;
 * </pre></blockquote></li>
 *  <li>Add the <code>log4j</code> prefix to all Log4J configuration
 *  elements:
 *  <code>&lt;log4j:appender&gt;...&lt;/log4j:appender&gt;</code></li>
 * </ul>
 * <p>
 * To split a Log4J configuration across multiple files, use the W3C
 * <a href="http://www.w3.org/TR/2006/REC-xinclude-20061115/">XInclude</a>
 * syntax. The included file must be a valid XML document. To
 * declare several configuration items (e.g. appenders, loggers...) in
 * an included file, wrap them into an additional
 * <code>&lt;configuration&gt;</code> root element.<br/>
 * In addition to the usual URI schemes (<code>file:</code>,
 * <code>http:</code>...) it is possible to use the
 * <code>classpath:</code> scheme to reference classpath files in the
 * <code>href</code> attribute of the <code>&lt;include&gt;</code>
 * XInclude tag.<br/>
 * For example:</p>
 * <p><code>log4j.xml</code>:</p>
 * <blockquote><pre>
 * &lt;configuration debug="true" xmlns="http://jakarta.apache.org/log4j/"&gt;
 *   &lt;appender name="consoleLog" class="org.apache.log4j.ConsoleAppender"&gt;
 *     &lt;param name="target" value="System.out"/&gt;
 *     &lt;layout class="org.apache.log4j.PatternLayout"&gt;
 *       &lt;param name="conversionPattern" value="%d{ISO8601}|%p|%m%n"/&gt;
 *     &lt;/layout&gt;
 *   &lt;/appender&gt;
 *
 *   &lt;!-- load externalized logger definitions from classpath --&gt;
 *   &lt;xi:include xmlns:xi="http://www.w3.org/2001/XInclude"
 *               href="classpath:conf/log4j-loggers.xml"&gt;
 *     &lt;xi:fallback&gt;
 *       &lt;!-- Fallback: no debug --&gt;
 *       &lt;logger name="com.mypackage"&gt;
 *         &lt;level value="INFO"/&gt;
 *       &lt;/logger&gt;
 *     &lt;/xi:fallback&gt;
 *   &lt;/xi:include&gt;
 *
 *   &lt;root&gt;
 *     &lt;level value="DEBUG"/&gt;
 *     &lt;appender-ref ref="consoleLog"/&gt;
 *   &lt;/root&gt;
 * &lt;/configuration&gt;
 * </pre></blockquote>
 * <p><code>log4j-loggers.xml</code>:</p>
 * <blockquote><pre>
 * &lt;configuration xmlns="http://jakarta.apache.org/log4j/"&gt;
 *   &lt;!-- Use trace level for tests --&gt;
 *   &lt;logger name="com.mypackage"&gt;
 *     &lt;level value="TRACE"/&gt;
 *   &lt;/logger&gt;
 *   &lt;logger name="org.summerframework"&gt;
 *     &lt;level value="WARN"/&gt;
 *   &lt;/logger&gt;
 * &lt;/configuration&gt;
 * </pre></blockquote>
 * <p>
 * Configuration data support variable substitution.  The
 * syntax of variable substitution is similar to that of Unix
 * shells.  The string between an opening <b>&quot;${&quot;</b> and
 * closing <b>&quot;}&quot;</b> is interpreted as a key.  The value of
 * the substituted variable can be defined in the XML configuration
 * using the <code>&lt;variable name="foo" value="bar"/&gt;</code> tag,
 * as a system property or in a
 * {@link #setProperties provided properties object}.  The value of
 * the key is searched in the system properties first, in the Log4J
 * configuration variables and finally in the provided properties.  The
 * corresponding value replaces the ${variableName} sequence. For
 * example, if <code>java.home</code> system property is set to
 * <code>/home/xyz</code>, then every occurrence of the sequence
 * <code>${java.home}</code> will be interpreted as
 * <code>/home/xyz</code>.  The scope of the configuration variables
 * is limited to the <code>&lt;configuration&gt;</code> element in
 * which they are defined; variables declared in a nested configuration
 * element hide those defined in the parent elements.</p>
 * <p>
 * Configuration update check can be activated directly in the Log4J
 * configuration by setting the <code>updateWatchdog</code> attribute
 * to the first-level <code>&lt;configuration&gt;</code> tag to a
 * positive integer value: the update check interval as a number of
 * seconds.<br />
 * <i>Warning:</i> In case the XInclude fallback feature is used, the
 * configurator can monitor the creation of not-found files if some
 * elements of the file path (e.g. the <code>conf</code> parent
 * directory in the above configuration example) exists at the time
 * the configuration was first loaded. In that case, the first matching
 * longest path found is monitored. If no file path elements can be
 * found, files can not be monitored for addition.<br />
 * The minimum check interval is 1 minute (60 seconds). if the
 * configured check interval is lower, this minimum value is
 * assumed.</p>
 *
 * @author lbihanic
 */
@SuppressWarnings("unchecked")                  // Log4J is 1.4 compatible
public class DOMConfigurator2 extends DOMConfigurator
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** URI of the Log4J XML namespace. */
    public final static String LOG4J_NS_URI =
                                            "http://jakarta.apache.org/log4j/";
    /**
     * Name of the system property to set to request XML validation
     * of the Log4J configuration
     */
    public final static String VALIDATE_CONFIGURATION_KEY =
                                            "log4j.validateConfiguration";

    // XML tag local and namespace-qualified names.
    private final static String NEW_CONFIGURATION_TAG   = "configuration";
    private final static String THROWABLE_RENDERER_TAG  = "throwableRenderer";
    private final static String CATEGORY_TAG      = CATEGORY;
    private final static String LOGGER_TAG        = LOGGER;
    private final static String RESET_ATTR        = "reset";
    private final static String VARIABLE_TAG      = "variable";
    private final static String UPDATE_WATCHDOG_ATTR = "updateWatchdog";

    private final static String CONFIGURATION_QNAME =
                                              getQName(NEW_CONFIGURATION_TAG);
    private final static String CATEGORY_QNAME  = getQName(CATEGORY_TAG);
    private final static String LOGGER_QNAME    = getQName(LOGGER_TAG);
    private final static String ROOT_QNAME      = getQName(ROOT_TAG);
    private final static String APPENDER_QNAME  = getQName(APPENDER_TAG);
    private final static String RENDERER_QNAME  = getQName(RENDERER_TAG);
    private final static String CATEGORY_FACTORY_QNAME =
                                                getQName(CATEGORY_FACTORY_TAG);
    private final static String LOGGER_FACTORY_QNAME =
                                                getQName(LOGGER_FACTORY_TAG);
    private final static String VARIABLE_QNAME    = getQName(VARIABLE_TAG);

    private final static String LEVEL_QNAME     = getQName(LEVEL_TAG);
    private final static String PRIORITY_QNAME  = getQName(PRIORITY_TAG);
    private final static String PARAM_QNAME     = getQName(PARAM_TAG);
    private final static String LAYOUT_QNAME    = getQName(LAYOUT_TAG);
    private final static String FILTER_QNAME    = getQName(FILTER_TAG);
    private final static String ERROR_HANDLER_QNAME =
                                                getQName(ERROR_HANDLER_TAG);
    private final static String LOGGER_REF_QNAME  = getQName(LOGGER_REF);
    private final static String ROOT_REF_QNAME    = getQName(ROOT_REF);
    private final static String APPENDER_REF_QNAME =
                                                getQName(APPENDER_REF_TAG);
    private final static String THROWABLE_RENDERER_QNAME =
                                            getQName(THROWABLE_RENDERER_TAG);

    /** JAXP schema language property id. */
    private final static String JAXP_SCHEMA_LANGUAGE_KEY =
                    "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    /** JAXP schema location property id. */
    private final static String JAXP_SCHEMA_LOCATION_KEY =
                    "http://java.sun.com/xml/jaxp/properties/schemaSource";
    /** W3C XML Schema language URI */
    private final static String W3C_XML_SCHEMA_NS_URI =
                                        "http://www.w3.org/2001/XMLSchema";
    /** URI scheme for file entities. */
    private final static String FILE_URI_SCHEME = "file:";
    /** URI scheme for classpath entities. */
    private final static String CLASSPATH_URI_SCHEME = "classpath:";
    /** Log4J XML schema path in JAR. */
    private final static String LOG4J_XSD_PATH =
                                        "org/apache/log4j/xml/log4j-1.2.xsd";
    /** Key for DOM node user data to mark already processed node. */
    private final static String PROCESSED_TAG_KEY = "log4:processed";

    private final static int MIN_UPDATE_WATCHDOG = 60;  // 1 minute

    //-------------------------------------------------------------------------
    // Class members definition
    //-------------------------------------------------------------------------

    /** Timer for configuration file watchdogs. */
    private static Timer fileWatchdogTimer = null;

    //-------------------------------------------------------------------------
    // Instance members definition
    //-------------------------------------------------------------------------

    /** User provider configuration properties, if any. */
    private Properties userProps = null;
    /** Active configuration file watchdogs for file update checks. */
    private final Collection fileWatchdogs = new LinkedList();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public DOMConfigurator2() {
        super();
    }

    //-------------------------------------------------------------------------
    // Configurator interface support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    public final void doConfigure(final URL url, LoggerRepository repository) {
        if (url == null) {
            throw new IllegalArgumentException("url");
        }
        InputSource src = null;
        try {
            URLConnection cnx = url.openConnection();
            cnx.setUseCaches(false);
            src = new InputSource(cnx.getInputStream());
            src.setSystemId(url.toString());
        }
        catch (IOException e) {
            if (e instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LogLog.error("Could not parse Log4J configuration at "
                         + src + ".", e);
        }

        this.doConfigure(src, repository);
    }

    //-------------------------------------------------------------------------
    // DOMConfigurator interface support
    //-------------------------------------------------------------------------

    /**
     * Configures Log4J from the specified XML configuration file.
     *
     * @param  fileName     the (relative) path to the Log4J XML
     *                      configuration file.
     * @param  repository   the repository to add configured elements
     *                      to.
     */
    public final void doConfigure(final String fileName,
                                  LoggerRepository repository) {
        if ((fileName == null) || (fileName.length() == 0)) {
            throw new IllegalArgumentException("url");
        }
        File src = new File(fileName);
        if (! (src.isFile() && src.canRead())) {
            throw new IllegalArgumentException(
                        new FileNotFoundException(src.toString()));
        }
        this.doConfigure(new InputSource(src.toString()), repository);
    }

    /**
     * Configures Log4J from the specified byte stream using the
     * platform default character encoding (unless an encoding is
     * specified in the XML data (BOM headers or XML prefix)).
     *
     * @param  inputStream   the byte stream to read XML configuration
     *                       data from.
     * @param  repository    the repository to add configured elements
     *                       to.
     */
    public final void doConfigure(final InputStream inputStream,
                                  LoggerRepository repository) {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream");
        }
        this.doConfigure(inputStream, null, repository);
    }

    /**
     * Configures Log4J from the specified character stream.
     *
     * @param  reader       the character stream to read XML
     *                      configuration data from.
     * @param  repository   the repository to add configured elements
     *                      to.
     */
    public final void doConfigure(final Reader reader,
                                  LoggerRepository repository) {
        if (reader == null) {
            throw new IllegalArgumentException("reader");
        }
        this.doConfigure(new InputSource(reader), repository);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Returns the properties object against which variables (expressed
     * as "<code>${var}</code>") found in configuration data are
     * resolved.
     *
     * @return the properties object against which variables are
     *         resolved or <code>null</code> if none has been set.
     */
    public Properties getProperties() {
        return this.userProps;
    }

    /**
     * Sets the properties object against which variables (expressed
     * as "<code>${var}</code>") found in configuration data are
     * resolved.
     * <p>
     * Variables are first resolved against the system properties and
     * if not found against the specified properties.</p>
     *
     * @param  props   the properties object against which resolving
     *                 variables or <code>null</code> to remove the
     *                 current object.
     */
    public void setProperties(Properties props) {
        this.userProps = props;
        this.props = this.userProps;
    }

    /**
     * Configures Log4J from the specified byte stream using the
     * specified character encoding.
     *
     * @param  inputStream   the byte stream to read XML configuration
     *                       data from.
     * @param  encoding      the character encoding or <code>null</code>
     *                       if not known.
     * @param  repository    the repository to add configured elements
     *                       to.
     */
    public final void doConfigure(final InputStream inputStream,
                                  final String encoding,
                                  LoggerRepository repository) {
        InputSource src = new InputSource(inputStream);
        src.setEncoding(encoding);

        this.doConfigure(src, repository);
    }

    /**
     * Configures Log4J from the specified XML source.
     *
     * @param  source       the Log4J XML configuration.
     * @param  repository   the repository to add configured elements
     *                      to.
     */
    public void doConfigure(final InputSource source,
                            LoggerRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("repository");
        }
        this.repository = repository;

        this.doConfigure(source);
    }

    /**
     * Configures the current Log4J logger repository from the
     * specified XML source.
     *
     * @param  source   the Log4J XML configuration.
     */
    private void doConfigure(final InputSource source) {
        if (source == null) {
            throw new IllegalArgumentException("source");
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Enforce use of XML Namespaces
            dbf.setNamespaceAware(true);
            // Activate XInclude feature to support multiple file configurations
            try {
                dbf.setXIncludeAware(true);
            }
            catch (Exception e) {
                LogLog.warn("XML include (XInclude) feature not available");
            }
            // Check for configuration schema-based validation option
            if (toBoolean(OptionConverter.getSystemProperty(
                                    VALIDATE_CONFIGURATION_KEY, null), false)) {
                try {
                    // Request document validation
                    dbf.setValidating(true);
                    dbf.setAttribute(JAXP_SCHEMA_LANGUAGE_KEY,
                                     W3C_XML_SCHEMA_NS_URI);
                    // Validate against Log4J if present in classpath
                    URL schemaUrl = findResource(LOG4J_XSD_PATH);
                    if (schemaUrl != null) {
                        dbf.setAttribute(JAXP_SCHEMA_LOCATION_KEY,
                                         schemaUrl.toString());
                    }
                }
                catch (Exception e) {
                    LogLog.warn("Schema-based validation not available.");
                }
            }
            // Parse XML configuration, resolving included files.
            ClasspathEntityResolver resolver = new ClasspathEntityResolver();
            DocumentBuilder parser = dbf.newDocumentBuilder();
            // Use custom error handler to prevent console logging of XML
            // parse warnings issued when XInclude fallback occurs.
            parser.setErrorHandler(new SAXErrorHandler() {
                    public void warning(final SAXParseException e) {
                        // Ignore warnings...
                    }
                });
            parser.setEntityResolver(resolver);
            Document doc = parser.parse(source);

            // Reset known appenders.
            this.appenderBag.clear();
            // Stop all running file watchdogs as the reloaded
            // configurations may add or remove included files.
            for (Iterator i=this.fileWatchdogs.iterator(); i.hasNext(); ) {
                TimerTask t = (TimerTask)(i.next());
                t.cancel();
                i.remove();
            }
            // Apply configuration. Configuration is eligible for reloading
            // only if a valid system id. URI is provided.
            this.parse(doc, source.getSystemId(), resolver.getResolvedFiles());
        }
        catch (Exception e) {
            if ((e instanceof InterruptedException) ||
                (e instanceof InterruptedIOException)) {
                Thread.currentThread().interrupt();
            }
            LogLog.error("Could not parse Log4J configuration at "
                         + source + ".", e);
        }
    }

    /**
     * Analyses a DOM document, loading all Log4J configuration tags,
     * regardless their depth, and adding the configured elements to
     * the current repository.
     *
     * @param  doc   the DOM document to analyze.
     */
    protected void parse(Document doc) {
        this.parse(doc, null, null);
    }

    /**
     * Used internally to configure the Log4J framework from a DOM
     * elements.
     */
    protected void parse(Element element) {
        this.parse(element, true, null, null);
    }

    /**
     * Analyzes a DOM document, loading all Log4J configuration tags,
     * regardless their depth, and adding the configured elements to
     * the current repository.
     *
     * @param  doc             the DOM document to analyze.
     * @param  configUri       the URI (URL or file path) of the main
     *                         Log4J configuration.
     * @param  resolvedFiles   list of the resolved included
     *                         {@link java.io.File files}.
     */
    private void parse(Document doc, String configUri,
                                     Collection resolvedFiles) {
        if (doc == null) {
            throw new IllegalArgumentException("doc");
        }
        // Get all configuration root elements present in loaded document.
        NodeList cfgs = doc.getElementsByTagNameNS(
                                        LOG4J_NS_URI, NEW_CONFIGURATION_TAG);
        if (cfgs.getLength() == 0) {
            // Oops! At least one configuration element is required.
            LogLog.error("Configuration root element <"
                         + NEW_CONFIGURATION_TAG + "> not found.");
        }
        else {
            boolean first = true;
            // Load configurations in document order.
            for (int i=0, max=cfgs.getLength(); i<max; i++) {
                this.parse((Element)(cfgs.item(i)), first,
                                                    configUri, resolvedFiles);
                first = false;
            }
        }
    }

    /**
     * Analyzes a Log4J configuration root element and configures the
     * declared items.
     * <p>
     * The configuration element attributes (debug, reset and
     * threshold) are ignored for embedded (i.e. non first level)
     * configuration elements.</p>
     *
     * @param  root         the Log4J configuration DOM node.
     * @param  firstLevel   whether this configuration element is a
     *                      first level tag or is embedded within
     *                      another configuration tag.
     */
    private final void parse(Element root, boolean firstLevel,
                             String configUri, Collection resolvedFiles) {
        // Check that element has not been already been processed.
        if ((root == null) ||
            (root.getUserData(PROCESSED_TAG_KEY) != null)) return;
        // Push a new variable context.
        VariableContext ctx = this.pushContext();

        if (firstLevel) {
            // Only first level configuration tags can define the
            // debug flag, threshold level and reset configuration.
            String attr = subst(root.getAttribute(INTERNAL_DEBUG_ATTR));
            if (toBoolean(attr, false)) {
                LogLog.setInternalDebugging(true);
            }
            attr = subst(root.getAttribute(RESET_ATTR));
            if (toBoolean(attr, false)) {
                LogLog.debug("Resetting configuration");
                repository.resetConfiguration();
            }
            attr = subst(root.getAttribute(THRESHOLD_ATTR));
            if ((! "".equals(attr)) && (! "null".equals(attr))) {
                LogLog.debug("Threshold=\"" + attr + '"');
                repository.setThreshold(attr);
            }
            attr = subst(root.getAttribute(UPDATE_WATCHDOG_ATTR));
            if (! "".equals(attr)) {
                int checkInterval = 0;
                try {
                    checkInterval = Integer.parseInt(attr);
                }
                catch (Exception e) {
                    LogLog.warn("Invalid " + UPDATE_WATCHDOG_ATTR
                                + " value: " + attr + " (integer expected)");
                }
                if (checkInterval > 0) {
                    this.startWatchdogs(checkInterval,
                                                    configUri, resolvedFiles);
                }
            }
        }
        // Revolve variables first.
        NodeList children = root.getChildNodes();
        for (int i=0, max=children.getLength(); i<max; i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element elt = (Element) n;

                if (VARIABLE_QNAME.equals(getQName(elt))) {
                    this.parseVariable(elt);
                }
            }
        }
        // Configure category factories first, i.e. before any category.
        for (int i=0, max=children.getLength(); i<max; i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element elt = (Element) n;

                if ((CATEGORY_FACTORY_QNAME.equals(getQName(elt))) ||
                    (LOGGER_FACTORY_QNAME.equals(getQName(elt)))) {
                    this.parseCategoryFactory(elt);
                }
            }
        }
        // Configure all other items, in document order.
        for (int i=0, max=children.getLength(); i<max; i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element elt = (Element)n;
                String tagName = getQName(elt);

                if ((CATEGORY_QNAME.equals(tagName)) ||
                    (LOGGER_QNAME.equals(tagName))) {
                    this.parseCategory(elt);
                }
                else if (ROOT_QNAME.equals(tagName)) {
                    this.parseRoot(elt);
                }
                else if (RENDERER_QNAME.equals(tagName)) {
                    this.parseRenderer(elt);
                }
                else if (THROWABLE_RENDERER_QNAME.equals(tagName)) {
                    if (this.repository instanceof ThrowableRendererSupport) {
                        ThrowableRenderer tr = this.parseThrowableRenderer(elt);
                        if (tr != null) {
                            ((ThrowableRendererSupport)repository)
                                                    .setThrowableRenderer(tr);
                        }
                    }
                }
                else if (CONFIGURATION_QNAME.equals(tagName)) {
                    this.parse(elt, false, null, null);
                }
                else if (!(APPENDER_QNAME.equals(tagName) ||
                           VARIABLE_QNAME.equals(tagName) ||
                           CATEGORY_FACTORY_QNAME.equals(tagName) ||
                           LOGGER_FACTORY_QNAME.equals(tagName))) {
                    quietParseUnrecognizedElement(this.repository,
                                                  elt, this.props);
                }
            }
        }
        // Pop variable context.
        this.popContext(ctx);
        // Mark this configuration element as processed.
        root.setUserData(PROCESSED_TAG_KEY, Boolean.TRUE, null);
    }

    /**
     * Adds a new variable context at the top of the context stack.
     *
     * @return the new variable context.
     */
    private VariableContext pushContext() {
        VariableContext ctx = new VariableContext(this.props);
        this.props = ctx;
        return ctx;
    }

    /**
     * Removed the specified variable context and its children from
     * the context stack.
     *
     * @param  ctx   the variable context to remove.
     */
    private void popContext(VariableContext ctx) {
        this.props = ctx.getParent();
    }

    /**
     * Used internally to parse variables definitions.
     *
     * @param  elt   the DOM Element to parse.
     */
    private void parseVariable(Element elt) {
        String rawKey = elt.getAttribute(NAME_ATTR);
        String key    = this.subst(rawKey);
        String value  = this.subst(elt.getAttribute(VALUE_ATTR));

        if ((key != null) && (key.trim().length() != 0)) {
            this.props.setProperty(key, value);
            LogLog.debug("Setting variable [" + key + "] to [" + value + ']');
        }
        else {
            LogLog.warn("Failed to revolved variable \"" + rawKey + '"');
        }
    }

    /**
     * Searches the Java VM classpath and the local file system for
     * the specified resource.
     *
     * @param  name   the resource path relative to the classpath.
     *
     * @return the URL of the specified resource in the classpath or on
     *         the local file system; <code>null</code> if the resource
     *         was not found.
     */
    private final URL findResource(String name) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = this.getClass().getClassLoader();
        }
        URL u = cl.getResource(name);
        if (u == null) {
            // Not found in classpath. Try as a local file.
            File f = new File(name);
            if (f.exists()) {
                try {
                    u = f.toURI().toURL();
                }
                catch (Exception e) { /* Can't happen here. */ }
            }
        }
        return u;
    }

    /**
     * Starts the configuration file update watchdogs for the main
     * Log4J configuration file (if is is a local file) and every
     * resolved included file.
     *
     * @param  checkInterval   the file check interval read from the
     *                         Log4J configuration.
     * @param  configUri       the URI (URL or file path) of the main
     *                         Log4J configuration.
     * @param  resolvedFiles   list of the resolved included
     *                         {@link java.io.File files}.
     */
    private void startWatchdogs(int checkInterval, String configUri,
                                                   Collection resolvedFiles) {
        if ((checkInterval > 0) && (configUri != null)) {
            if (checkInterval < MIN_UPDATE_WATCHDOG) {
                checkInterval = MIN_UPDATE_WATCHDOG;
            }
            LogLog.debug("File update check interval: "
                                                + checkInterval + " seconds");
            long period = checkInterval * 1000L;

            // Watch main configuration file, if local.
            File f = null;
            try {
                f = new File(new URI(configUri));
            }
            catch (Exception e) {
                f = new File(configUri);
            }
            if (f.isFile() && f.canRead()) {
                this.addFileWatchdog(f, configUri, period);
            }
            // And all resolved included files.
            for (Iterator i=resolvedFiles.iterator(); i.hasNext(); ) {
                this.addFileWatchdog((File)(i.next()), configUri, period);
            }
        }
        // Else: No update check requested or configuration source not
        //       supporting being read multiple times (InputStream/Reader).
    }

    /**
     * Starts a file update watchdog.
     *
     * @param  f           the file to monitor.
     * @param  configUri   the URI of the main Log4J configuration.
     * @param  period      the file check interval.
     */
    private void addFileWatchdog(File f, String configUri, long period) {
        FileWatchdog watchdog = new FileWatchdog(f, configUri);
        getTimer().schedule(watchdog, period, period);
        this.fileWatchdogs.add(watchdog);
        LogLog.debug("Added file update watchdog for " + f);
    }

    /**
     * Reloads the specified Log4J configuration.
     *
     * @param  configUri   the URI of the Log4J configuration.
     */
    private void reload(String configUri) {
        this.doConfigure(new InputSource(configUri));
    }

    /**
     * Parses a string conveying a boolean value.
     *
     * @param  value   the string value to parse.
     * @param  def     the value to return if the specified string can
     *                 not be interpreted as a valid boolean value.
     *
     * @return <code>true</code> if the specified string contains
     *         "<code>true</code>", "<code>yes</code>" or
     *         "<code>1</code>", <code>false</code> if it contains
     *         "<code>false</code>", "<code>no</code>" or
     *         "<code>0</code>" and the default value otherwise.
     */
    private final static boolean toBoolean(String value, boolean def) {
        boolean b = def;
        if (value != null) {
            value = value.trim();
            if (("true".equalsIgnoreCase(value)) ||
                ("yes".equalsIgnoreCase(value))  || ("1".equals(value))) {
                b = true;
            }
            else if (("false".equalsIgnoreCase(value)) ||
                     ("no".equalsIgnoreCase(value)) || ("0".equals(value))) {
                b = false;
            }
        }
        return b;
    }

    /**
     * Return the string representation of an XML element in the form:
     * <code>{&lt;http://jakarta.apache.org/log4j/&gt;}&lt;element name&gt;</code>.
     *
     * @param  name   the element name.
     *
     * @return the qualified name of the element or <code>null</code>
     *         if node is <code>null</code>.
     */
    private final static String getQName(String name) {
        return getQName(LOG4J_NS_URI, name);
    }

    /**
     * Return the string representation of an XML element in the form:
     * <code>{&lt;namespace URI&gt;}&lt;element name&gt;</code>.
     *
     * @param  ns     the element namespace URI.
     * @param  name   the element name.
     *
     * @return the qualified name of the element or <code>null</code>
     *         if node is <code>null</code>.
     *
     * @see    #getQName(Element)
     */
    private final static String getQName(String nsUri, String name) {
        return ((nsUri != null) && (nsUri.length() != 0))?
                                                "{" + nsUri + '}' + name: name;
    }

    /**
     * Return the string representation of a DOM element in the form:
     * <code>{&lt;namespace URI&gt;}&lt;element name&gt;</code>.
     *
     * @param  node   the DOM element.
     *
     * @return the qualified name of the element or <code>null</code>
     *         if node is <code>null</code>.
     *
     * @see    #getQName(Element)
     */
    private final static String getQName(Element node) {
        String qName = null;
        if (node != null) {
            qName = getQName(node.getNamespaceURI(), node.getLocalName());
        }
        return qName;
    }

    private static Timer getTimer() {
        if (fileWatchdogTimer == null) {
            fileWatchdogTimer = new Timer(
                        "Log4J DOMConfigurator2 File Update Watchdog", true);
        }
        return fileWatchdogTimer;
    }

    //-------------------------------------------------------------------------
    // Imported DOMConfigurator private methods
    //-------------------------------------------------------------------------

    /**
     * Delegates unrecognized content to created instance if
     * it supports UnrecognizedElementParser.
     * @param instance instance, may be null.
     * @param element element, may not be null.
     * @param props properties
     * @throws IOException thrown if configuration of owner object
     * should be abandoned.
     */
    private static void parseUnrecognizedElement(final Object instance,
                                    final Element element,
                                    final Properties props) throws Exception {
        boolean recognized = false;
        if (instance instanceof UnrecognizedElementHandler) {
            recognized = ((UnrecognizedElementHandler)instance)
                                    .parseUnrecognizedElement(element, props);
        }
        if (!recognized) {
            LogLog.warn("Unrecognized element: " + element.getTagName());
        }
    }

    /**
     * Delegates unrecognized content to created instance if
     * it supports UnrecognizedElementParser and catches and
     *  logs any exception.
     * @param instance instance, may be null.
     * @param element element, may not be null.
     * @param props properties
     */
    private static void quietParseUnrecognizedElement(final Object instance,
                                                      final Element element,
                                                      final Properties props) {
        try {
            parseUnrecognizedElement(instance, element, props);
        }
        catch (Exception e) {
          if ((e instanceof InterruptedException) ||
              (e instanceof InterruptedIOException)) {
              Thread.currentThread().interrupt();
          }
            LogLog.error("Error in extension content: ", e);
        }
    }

    //-------------------------------------------------------------------------
    // Overridden DOMConfigurator protected methods
    //  -> Replacement of Element.getTagName() by the tag qualified name,
    //     to ignore actual namespace prefix used in document, if any.
    //-------------------------------------------------------------------------

  /**
     Used internally to parse appenders by IDREF name.
  */
  protected
  Appender findAppenderByName(Document doc, String appenderName)  {
    Appender appender = (Appender) appenderBag.get(appenderName);

    if(appender != null) {
      return appender;
    } else {
      // Doesn't work on DOM Level 1 :
      // Element element = doc.getElementById(appenderName);

      // Endre's hack:
      Element element = null;
      NodeList list = doc.getElementsByTagNameNS(LOG4J_NS_URI, APPENDER_TAG);
      for (int t=0; t < list.getLength(); t++) {
	Node node = list.item(t);
	NamedNodeMap map = node.getAttributes();
	Node attrNode = map.getNamedItem(NAME_ATTR);
	if (appenderName.equals(attrNode.getNodeValue())) {
	  element = (Element) node;
	  break;
	}
      }
      // Hack finished.

      if(element == null) {
	LogLog.error("No appender named ["+appenderName+"] could be found.");
	return null;
      } else {
          appender = parseAppender(element);
          if (appender != null) {
            appenderBag.put(appenderName, appender);
          }
	return appender;
      }
    }
  }

  /**
     Used internally to parse an appender element.
   */
  protected
  Appender parseAppender (Element appenderElement) {
    String className = subst(appenderElement.getAttribute(CLASS_ATTR));
    LogLog.debug("Class name: [" + className+']');
    try {
      Object instance 	= Loader.loadClass(className).newInstance();
      Appender appender	= (Appender)instance;
      PropertySetter propSetter = new PropertySetter(appender);

      appender.setName(subst(appenderElement.getAttribute(NAME_ATTR)));

      NodeList children	= appenderElement.getChildNodes();
      final int length 	= children.getLength();

      for (int loop = 0; loop < length; loop++) {
	Node currentNode = children.item(loop);

	/* We're only interested in Elements */
	if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
	  Element currentElement = (Element)currentNode;
	  String tagName = getQName(currentElement);

	  // Parse appender parameters
	  if (PARAM_QNAME.equals(tagName)) {
            setParameter(currentElement, propSetter);
	  }
	  // Set appender layout
	  else if (LAYOUT_QNAME.equals(tagName)) {
	    appender.setLayout(parseLayout(currentElement));
	  }
	  // Add filters
	  else if (FILTER_QNAME.equals(tagName)) {
	    parseFilters(currentElement, appender);
	  }
	  else if (ERROR_HANDLER_QNAME.equals(tagName)) {
	    parseErrorHandler(currentElement, appender);
	  }
	  else if (APPENDER_REF_QNAME.equals(tagName)) {
	    String refName = subst(currentElement.getAttribute(REF_ATTR));
	    if(appender instanceof AppenderAttachable) {
	      AppenderAttachable aa = (AppenderAttachable) appender;
	      LogLog.debug("Attaching appender named ["+ refName+
			   "] to appender named ["+ appender.getName()+"].");
	      aa.addAppender(findAppenderByReference(currentElement));
	    } else {
	      LogLog.error("Requesting attachment of appender named ["+
			   refName+ "] to appender named ["+ appender.getName()+
                "] which does not implement org.apache.log4j.spi.AppenderAttachable.");
	    }
	  } else {
          parseUnrecognizedElement(instance, currentElement, props);
      }
	}
      }
      propSetter.activate();
      return appender;
    }
    /* Yes, it's ugly.  But all of these exceptions point to the same
       problem: we can't create an Appender */
    catch (Exception oops) {
        if (oops instanceof InterruptedException || oops instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
        }
      LogLog.error("Could not create an Appender. Reported error follows.",
		   oops);
      return null;
    }
  }

  /**
     Used internally to parse an {@link ErrorHandler} element.
   */
  protected
  void parseErrorHandler(Element element, Appender appender) {
    ErrorHandler eh = (ErrorHandler) OptionConverter.instantiateByClassName(
                                       subst(element.getAttribute(CLASS_ATTR)),
                                       org.apache.log4j.spi.ErrorHandler.class,
 				       null);
    if(eh != null) {
      eh.setAppender(appender);

      PropertySetter propSetter = new PropertySetter(eh);
      NodeList children = element.getChildNodes();
      final int length 	= children.getLength();

      for (int loop = 0; loop < length; loop++) {
	Node currentNode = children.item(loop);
	if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
	  Element currentElement = (Element) currentNode;
	  String tagName = getQName(currentElement);

	  if(PARAM_QNAME.equals(tagName)) {
            setParameter(currentElement, propSetter);
	  } else if(APPENDER_REF_QNAME.equals(tagName)) {
	    eh.setBackupAppender(findAppenderByReference(currentElement));
	  } else if(LOGGER_REF_QNAME.equals(tagName)) {
	    String loggerName = currentElement.getAttribute(REF_ATTR);
	    Logger logger = (catFactory == null) ? repository.getLogger(loggerName)
                : repository.getLogger(loggerName, catFactory);
	    eh.setLogger(logger);
	  } else if(ROOT_REF_QNAME.equals(tagName)) {
	    Logger root = repository.getRootLogger();
	    eh.setLogger(root);
	  } else {
          quietParseUnrecognizedElement(eh, currentElement, props);
      }
	}
      }
      propSetter.activate();
      appender.setErrorHandler(eh);
    }
  }

  /**
     Used internally to parse a filter element.
   */
  protected
  void parseFilters(Element element, Appender appender) {
    String clazz = subst(element.getAttribute(CLASS_ATTR));
    Filter filter = (Filter) OptionConverter.instantiateByClassName(clazz,
                                                Filter.class, null);

    if(filter != null) {
      PropertySetter propSetter = new PropertySetter(filter);
      NodeList children = element.getChildNodes();
      final int length 	= children.getLength();

      for (int loop = 0; loop < length; loop++) {
	Node currentNode = children.item(loop);
	if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
	  Element currentElement = (Element) currentNode;
	  String tagName = getQName(currentElement);

	  if(PARAM_QNAME.equals(tagName)) {
            setParameter(currentElement, propSetter);
	  } else {
            quietParseUnrecognizedElement(filter, currentElement, props);
      }
	}
      }
      propSetter.activate();
      LogLog.debug("Adding filter of type ["+filter.getClass()
		   +"] to appender named ["+appender.getName()+"].");
      appender.addFilter(filter);
    }
  }

  /**
     Used internally to parse the category factory element.
  */
  protected
  void parseCategoryFactory(Element factoryElement) {
    String className = subst(factoryElement.getAttribute(CLASS_ATTR));

    if(EMPTY_STR.equals(className)) {
      LogLog.error("Category Factory tag " + CLASS_ATTR + " attribute not found.");
      LogLog.debug("No Category Factory configured.");
    }
    else {
      LogLog.debug("Desired category factory: ["+className+']');
      Object factory = OptionConverter.instantiateByClassName(className,
                                                                 LoggerFactory.class,
                                                                 null);
      if (factory instanceof LoggerFactory) {
          catFactory = (LoggerFactory) factory;
      } else {
          LogLog.error("Category Factory class " + className + " does not implement org.apache.log4j.LoggerFactory");
      }
      PropertySetter propSetter = new PropertySetter(factory);

      Element  currentElement = null;
      Node     currentNode    = null;
      NodeList children       = factoryElement.getChildNodes();
      final int length        = children.getLength();

      for (int loop=0; loop < length; loop++) {
        currentNode = children.item(loop);
	if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
	  currentElement = (Element)currentNode;
	  if (PARAM_QNAME.equals(getQName(currentElement))) {
	    setParameter(currentElement, propSetter);
	  } else {
           quietParseUnrecognizedElement(factory, currentElement, props);
      }
	}
      }
    }
  }

  /**
     Used internally to parse the children of a category element.
  */
  protected
  void parseChildrenOfLoggerElement(Element catElement,
				      Logger cat, boolean isRoot) {
    PropertySetter propSetter = new PropertySetter(cat);

    // Remove all existing appenders from cat. They will be
    // reconstructed if need be.
    cat.removeAllAppenders();


    NodeList children 	= catElement.getChildNodes();
    final int length 	= children.getLength();

    for (int loop = 0; loop < length; loop++) {
      Node currentNode = children.item(loop);

      if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
	Element currentElement = (Element) currentNode;
	String tagName = getQName(currentElement);

	if (APPENDER_REF_QNAME.equals(tagName)) {
	  Element appenderRef = (Element) currentNode;
	  Appender appender = findAppenderByReference(appenderRef);
	  String refName =  subst(appenderRef.getAttribute(REF_ATTR));
	  if(appender != null)
	    LogLog.debug("Adding appender named ["+ refName+
			 "] to category ["+cat.getName()+"].");
	  else
	    LogLog.debug("Appender named ["+ refName + "] not found.");

	  cat.addAppender(appender);

	} else if(LEVEL_QNAME.equals(tagName)) {
	  parseLevel(currentElement, cat, isRoot);
	} else if(PRIORITY_QNAME.equals(tagName)) {
	  parseLevel(currentElement, cat, isRoot);
	} else if(PARAM_QNAME.equals(tagName)) {
          setParameter(currentElement, propSetter);
	} else {
        quietParseUnrecognizedElement(cat, currentElement, props);
    }
      }
    }
    propSetter.activate();
  }

  /**
     Used internally to parse a layout element.
  */
  protected
  Layout parseLayout (Element layout_element) {
    String className = subst(layout_element.getAttribute(CLASS_ATTR));
    LogLog.debug("Parsing layout of class: \""+className+"\"");
    try {
      Object instance 	= Loader.loadClass(className).newInstance();
      Layout layout   	= (Layout)instance;
      PropertySetter propSetter = new PropertySetter(layout);

      NodeList params 	= layout_element.getChildNodes();
      final int length 	= params.getLength();

      for (int loop = 0; loop < length; loop++) {
	Node currentNode = (Node)params.item(loop);
	if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
	  Element currentElement = (Element) currentNode;
	  String tagName = getQName(currentElement);

	  if(PARAM_QNAME.equals(tagName)) {
            setParameter(currentElement, propSetter);
	  } else {
          parseUnrecognizedElement(instance, currentElement, props);
      }
	}
      }

      propSetter.activate();
      return layout;
    }
    catch (Exception oops) {
        if (oops instanceof InterruptedException || oops instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
        }
      LogLog.error("Could not create the Layout. Reported error follows.",
		   oops);
      return null;
    }
  }

    /**
     * Parses throwable renderer.
     * @param element throwableRenderer element.
     * @return configured throwable renderer.
     * @since 1.2.16.
     */
    protected ThrowableRenderer parseThrowableRenderer(final Element element) {
        String className = subst(element.getAttribute(CLASS_ATTR));
        LogLog.debug("Parsing throwableRenderer of class: \""+className+"\"");
        try {
          Object instance 	= Loader.loadClass(className).newInstance();
          ThrowableRenderer tr   	= (ThrowableRenderer)instance;
          PropertySetter propSetter = new PropertySetter(tr);

          NodeList params 	= element.getChildNodes();
          final int length 	= params.getLength();

          for (int loop = 0; loop < length; loop++) {
                Node currentNode = (Node)params.item(loop);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element currentElement = (Element) currentNode;
                    String tagName = currentElement.getTagName();
                    if(PARAM_QNAME.equals(tagName)) {
                        setParameter(currentElement, propSetter);
                    } else {
                        parseUnrecognizedElement(instance, currentElement, props);
                    }
                }
          }

          propSetter.activate();
          return tr;
        }
        catch (Exception oops) {
            if (oops instanceof InterruptedException || oops instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LogLog.error("Could not create the ThrowableRenderer. Reported error follows.",
               oops);
          return null;
        }
    }

    //-------------------------------------------------------------------------
    // Specific entity resolver to support classpath URIs
    //-------------------------------------------------------------------------

    /**
     * A SAX EntityResolver that attempts to revoled entities against
     * the Java VM class path.
     */
    private final class ClasspathEntityResolver implements EntityResolver
    {
        private final Collection configFiles = new LinkedList();

        private final Pattern LOG4J_XSD_PATTERN =
                        Pattern.compile(".*?/log4j(-[0-9]+\\.[0-9]+)?.xsd");

        public ClasspathEntityResolver() {
            super();
        }

        /** {@inheritDoc} */
        public InputSource resolveEntity(String publicId, String systemId)
                                            throws SAXException, IOException {
            InputSource is = null;

            String localId = null;
            boolean configFile = false;

            if (LOG4J_XSD_PATTERN.matcher(systemId).matches()) {
                // Log4J configuration schema.
                localId = LOG4J_XSD_PATH;
            }
            else if ((systemId.endsWith("log4j.dtd")) ||
                     ("-//APACHE//DTD LOG4J 1.2//EN".equals(publicId))) {
                // Log4J configuration schema.
                localId = "org/apache/log4j/xml/log4j.dtd";
            }
            else if (systemId.startsWith(CLASSPATH_URI_SCHEME)) {
                // Classpath URI, not handled by XInclude processors.
                configFile = true;
                localId = systemId.substring(CLASSPATH_URI_SCHEME.length());
            }
            else if (systemId.startsWith(FILE_URI_SCHEME)) {
                // File URI: handled to manage configuration reload.
                configFile = true;
                localId = systemId.substring(FILE_URI_SCHEME.length());
            }
            if (localId != null) {
                URL resolved = findResource(localId);
                if (configFile) {
                    // Add file to the list of configuration files.
                    this.addConfigFile(localId, resolved);
                }
                if (resolved != null) {
                    is = new InputSource(resolved.toString());
                }
                else if (configFile) {
                    // Notify parser not to try to resolve the URL by itself.
                    throw new FileNotFoundException(
                                    "Included file " + localId + " not found");
                }
                // Else: return null to let the parser handle it.
            }
            return is;
        }

        /**
         * Returns an immutable list of the files resolved so far.
         *
         * @return the list of resolved Files.
         */
        public Collection getResolvedFiles() {
            return Collections.unmodifiableCollection(this.configFiles);
        }

        /**
         * Adds a file to the list of known configuration files. For
         * not-found files, i.e. when the XInclude fallback feature is
         * used, at least the start of the path shall exist.
         *
         * @param  path       the path of the file, as found in the
         *                    configuration, without the protocol part
         *                    (classpath: or file:).
         * @param  resolved   the URL of the file or <code>null</code>
         *                    if the file was not found in the classpath
         *                    or the local file system.
         */
        private void addConfigFile(String path, URL resolved) {
            String s = path;
            if (resolved == null) {
                // Resource not yet resolved (or not found).
                while ((s != null) && ((resolved = findResource(s)) == null)) {
                    // Not found. => Try with parent directory.
                    s = new File(s).getParent();
                }
            }
            if (resolved != null) {
                // Resource (or one of its parent directory) found locally.
                // => Add file to the list of resolved configuration files.
                try {
                    File f = new File(resolved.toURI());
                    if (f.canRead()) {
                        this.configFiles.add((s.equals(path))? f:
                                    new File(f, path.substring(s.length())));
                    }
                }
                catch (Exception e) { /* Can't happen here. */ }
            }
        }
    }

    //-------------------------------------------------------------------------
    // Specific Properties subclass to access parent properties (defaults)
    //-------------------------------------------------------------------------

    /**
     * A Properties subclass that allows accessing the parent
     * (defaults) Properties object.
     */
    private final static class VariableContext extends Properties
    {
        /**
         * Creates an empty property list with the specified parent
         * (defaults) Properties object.
         *
         * @param  defaults   the parent Properties object.
         */
        public VariableContext(Properties defaults) {
            super(defaults);
        }

        /**
         * Returns the parent Properties object.
         *
         * @return the parent Properties object.
         */
        public Properties getParent() {
            return this.defaults;
        }
    }

    //-------------------------------------------------------------------------
    // TimerTask to check for file updates
    //-------------------------------------------------------------------------

    /**
     * A TimerTask to check for file updates.
     */
    private final class FileWatchdog extends TimerTask
    {
        private final File monitoredFile;
        private final String configUri;
        private long lastModified = 0L;

        /**
         * Creates a new watchdog to check for updates of the specified
         * target file.
         *
         * @param  target      the file to monitor for updates.
         * @param  configUri   the URI of the Log4J configuration to
         *                     reload in case of update.
         */
        public FileWatchdog(File target, String configUri) {
            this.monitoredFile = target;
            this.configUri     = configUri;
            this.lastModified  = target.lastModified();
        }

        /**
         * Checks for target file update and reload the specified Log4J
         * configuration if need be.
         */
        public void run() {
            try {
                long l = this.monitoredFile.lastModified();
                if (l > this.lastModified) {
                    LogLog.debug("Watched file \"" + this.monitoredFile
                        + "\" updated. Reloading Log4J configuration from \""
                        + this.configUri + '"');
                    this.lastModified = l;
                    reload(this.configUri);
                }
            }
            catch (Exception e) { /* Ignore reloading failures. */ }
        }
    }
}


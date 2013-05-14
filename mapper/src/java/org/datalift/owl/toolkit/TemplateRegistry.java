package org.datalift.owl.toolkit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.rio.RDFFormat;

/**
 * Registers available Templates. Templates can be registered dynamically by looking into packages or directories; in that
 * case the registry will look for all the resources ending in -template.rdf, -template.n3, -template.trig or -template.trix.
 *
 * Templates can also be registered directly by passing in a stream or a Template object.
 * 
 * @author thomas
 *
 */
public class TemplateRegistry {

	private static Map<String, Template> templates = new Hashtable<String, Template>();

	static {
		// register standard template package with all sub packages
		// registerPackage("com.mondeca.sesame.toolkit.template");
	}

	/**
	 * @param image
	 * @return
	 */
	public static boolean hasTemplate(String templateName) {
		return templates.containsKey(templateName);
	}

	public static Template getTemplate(String templateName) {
		return templates.get(templateName);
	}

	/**
	 * Register a function package (will search for all classes in sub packages with FUNCTION_PREFIX as prefix)
	 * 
	 * @param pkg name
	 */
	public static void registerPackage(String pkg) {
		try {
			Enumeration<URL> list = TemplateRegistry.class.getClassLoader().getResources(pkg.replace('.', '/'));
			while (list.hasMoreElements()) {
				URL url = list.nextElement();
				System.out.println("Registering URL "+url.toString());
				if (url.getProtocol().equals("jar"))
					registerJarPackage(url, pkg);
				else if (url.getProtocol().equals("file"))
					registerFilePackage(new File(url.getFile()), pkg);
			}
		} catch (IOException e) {
		}
	}

	/**
	 * Recursively register sub packages from file system
	 *
	 * @param directory   base directory
	 * @param packageName package name for classes found inside the base directory
	 */
	@SuppressWarnings("unchecked")
	private static void registerFilePackage(File directory, String packageName) {
		if (!directory.exists())
			return;

		File[] files = directory.listFiles();
		for (File file : files) {
			String name = file.getName();
			if (file.isDirectory()) {
				if (!name.contains(".")) {
					registerFilePackage(file, packageName + "." + name);
				}
			} else if (name.endsWith("-template.rdf") || name.endsWith("-template.n3")|| name.endsWith("-template.trig")|| name.endsWith("-template.trix")) {
				String resourceName = packageName.replaceAll("\\.", "/") + "/" + name;
				register(resourceName);
			}
		}
	}

	/**
	 * Recursively register sub packages from JAR
	 *
	 * @param url
	 * @param packageName package name for classes found inside the base directory
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	private static void registerJarPackage(URL url, String packageName) throws IOException {
		String file = url.getFile();
		file = file.substring(5, file.indexOf("!"));
		JarFile jar = new JarFile(file);    	

		String pkg = packageName.replace('.', '/');    	
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();

			String fullName = entry.getName();
			String name = "";
			String thisPkg = "";
			Matcher m = Pattern.compile("^(.*)\\/(.*)$").matcher(fullName);
			if (m.find()) {
				thisPkg = m.group(1);
				name = m.group(2);
			}

			if (
					fullName.startsWith(pkg)
					&&
					(name.endsWith("-template.rdf") || name.endsWith("-template.n3")|| name.endsWith("-template.trig")|| name.endsWith("-template.trix"))
					) {
				register(pkg+"/"+name);
			}
		}
	}


	/**
	 * register a resource containing a template definition file
	 *  
	 */
	public static void register(String resourceName) {
		System.out.println("Registering template from resource : "+resourceName);
		TemplateParser parser = new TemplateParser();
		try {
			List<Template> parsedTemplates = parser.parseTemplates(TemplateRegistry.class.getClassLoader().getResourceAsStream(resourceName), RDFFormat.forFileName(resourceName));

			for (Template aTemplate : parsedTemplates) {
				register(aTemplate);
			}

		} catch (TemplateParsingException e) {
			e.printStackTrace();
		}

	}

	/**
	 * register a template object
	 *  
	 */
	public static void register(Template aTemplate) {
		System.out.println("Registering template with name : "+aTemplate.getName());
		templates.put(aTemplate.getName(),aTemplate);
	}

}

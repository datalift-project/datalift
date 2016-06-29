package org.datalift.geomrdf;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.FileSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.ShpSource;
import org.datalift.fwk.project.GmlSource;
import org.datalift.fwk.project.ShpSource.Crs;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.util.io.FileUtils;
import org.datalift.fwk.view.TemplateModel;

import fr.ign.datalift.GMLBuilder;
import fr.ign.datalift.parser.ShpParser;

import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.datalift.fwk.util.StringUtils.*;


/**
* A {@link ProjectModule project module} that loads the SHP data
* from a {@link ShpSource SHP file source} into the internal
* RDF store.
*
* @author fhamdi
*/
@Path(ShptoGml.MODULE_NAME)
public class ShptoGml extends BaseConverterModule
{
   //-------------------------------------------------------------------------
   // Constants
   //-------------------------------------------------------------------------

   /** The prefix for the URI of the project objects. */
   public final static String PROJECT_URI_PREFIX = "project";
   /** The prefix for the URI of the source objects, within projects. */
   public final static String SOURCE_URI_PREFIX  = "source";
   /** The relative path prefix for project objects and resources. */
   private final static String REL_PROJECT_PATH = PROJECT_URI_PREFIX + '/';
   /** The relative path prefix for source objects, within projects. */
   private final static String SOURCE_PATH = "/" + SOURCE_URI_PREFIX  + '/';

   /** The name of this module in the DataLift configuration. */
   public final static String MODULE_NAME = "shptogml";

   //-------------------------------------------------------------------------
   // Class members
   //-------------------------------------------------------------------------

   private final static Logger log = Logger.getLogger();

   //-------------------------------------------------------------------------
   // Constructors
   //-------------------------------------------------------------------------

   /** Default constructor. */
   public ShptoGml() {
       super(MODULE_NAME, 800, SourceType.ShpSource);
   }

   //-------------------------------------------------------------------------
   // Web services
   //-------------------------------------------------------------------------
    
   @GET
   @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
   public Response getIndexPage(@QueryParam("project") URI projectId) {
       // Retrieve project.
       Project p = this.getProject(projectId);
       // Display conversion configuration page.
       TemplateModel view = this.newView("shptoGml.vm", p);
       view.put("converter", this);
       view.put("crs", Crs.values());
       return Response.ok(view).build();
   }

   @POST
   @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
   public Response convertShpData(@FormParam("project") URI projectId,
           @FormParam("source") URI sourceId,
           @FormParam("crs") String crs)
                   throws WebApplicationException {
       Response response = null;
       try {
           // Retrieve project.
           Project p = this.getProject(projectId);
           // Retrieve source.
           ShpSource s = (ShpSource)(p.getSource(sourceId));
           if (s == null) {
               this.throwInvalidParamError("source", sourceId);
           }
           // Create working directory.
           File tmpDir = this.createWorkingDirectory(s);
           // Copy source ShapeFile files to working directory.
           File inShpFile = this.getLocalCopy(s.getShapeFilePath(),
                   s.getShapeFileInputStream(), tmpDir);
           this.getLocalCopy(s.getIndexFilePath(),
                   s.getIndexFileInputStream(), tmpDir);
           this.getLocalCopy(s.getAttributeFilePath(),
                   s.getAttributeFileInputStream(), tmpDir);
           this.getLocalCopy(s.getProjectionFilePath(),
                   s.getProjectionFileInputStream(), tmpDir);


           // Convert SHP to GML
           String rootName = inShpFile.getName();
           rootName = rootName.substring(0, rootName.lastIndexOf('.'));
           File outGmlFile = null;
           String fileOutGml = null;
           File outXsdFile = null;
           String fileOutXsd = null;
           if (Crs.valueOf(crs).getValue().equals("EPSG:4326")) {
               outGmlFile = new File(tmpDir, rootName + "_wgs84.gml");
               fileOutGml = outGmlFile.getName();
               if (outGmlFile.exists()) {
                   outGmlFile.delete();
               }
               outXsdFile = new File(tmpDir, rootName + "_wgs84.xsd");
               fileOutXsd = outXsdFile.getName();
               if (outXsdFile.exists()) {
                   outXsdFile.delete();
               }
               log.debug("Generating GML representation from WGS84 projection: {}", outGmlFile);
               GMLBuilder gml = new GMLBuilder();
               ShpParser shpfeatures = new ShpParser(inShpFile.getCanonicalPath(), true);
               gml.creatGMLFile(outXsdFile.getCanonicalPath(), outGmlFile.getCanonicalPath(), shpfeatures.featureSource);
           }
           else if (Crs.valueOf(crs).getValue().equals("none")) {
               outGmlFile = new File(tmpDir, rootName + ".gml");
               fileOutGml = outGmlFile.getName();
               if (outGmlFile.exists()) {
                   outGmlFile.delete();
               }
               outXsdFile = new File(tmpDir, rootName + ".xsd");
               fileOutXsd = outXsdFile.getName();
               if (outXsdFile.exists()) {
                   outXsdFile.delete();
               }
               log.debug("Generating GML representation from: {}", outGmlFile);
               GMLBuilder gml = new GMLBuilder();
               ShpParser shpfeatures = new ShpParser(inShpFile.getCanonicalPath(), false);
               gml.creatGMLFile(outXsdFile.getCanonicalPath(), outGmlFile.getCanonicalPath(), shpfeatures.featureSource);
           }

           // Build object URIs from request path.
           URI sourceUriGml = new URI(projectId.getScheme(), null,
                   projectId.getHost(), projectId.getPort(),
                   this.getSourceId(projectId.getPath(), fileOutGml),
                   null, null);

           // Initialize & persist new source.
           String filePathOutGml = this.getProjectFilePath(p.getTitle(), fileOutGml);
           String filePathOutXsd = this.getProjectFilePath(p.getTitle(), fileOutXsd);
           s.getFileStore().save(new FileInputStream(outGmlFile), filePathOutGml);
           s.getFileStore().save(new FileInputStream(outXsdFile), filePathOutXsd);
           GmlSource srcGml = this.projectManager.newGmlSource(p, sourceUriGml, fileOutGml,
                   "", filePathOutGml, filePathOutXsd);
           this.projectManager.saveProject(p);
           // Notify user of successful creation, redirecting HTML clients
           response = this.created(srcGml)
                   .build();
           log.info("New Gmlfile source \"{}\" created", fileOutGml);

       }
       catch (Exception e) {
           this.handleInternalError(e);
       }
       return response;
   }

   private String getProjectFilePath(String projectId, String fileName) {
       StringBuilder buf = new StringBuilder(80);
       buf.append(REL_PROJECT_PATH).append(urlify(projectId));
       if (isSet(fileName)) {
           buf.append('/').append(fileName);
       }
       return buf.toString();
   }

   private String getSourceId(String projectUri, String sourceName) {
       return projectUri + SOURCE_PATH + urlify(sourceName);
   }

   private File createWorkingDirectory(FileSource s) {
       String relPath = new File(s.getFilePath()).getParent();
       File tmpDir = new File(Configuration.getDefault().getTempStorage(),
               relPath);
       tmpDir.mkdirs();
       return tmpDir;
   }

   private File getLocalCopy(String path, InputStream in, File localDir)
           throws IOException {
       File f = new File(localDir, new File(path).getName());
       FileUtils.save(in, f);
       return f;
   }

}

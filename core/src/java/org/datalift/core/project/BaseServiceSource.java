package org.datalift.core.project;





import javax.persistence.MappedSuperclass;

import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ServiceSource;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.util.StringUtils;

import com.clarkparsia.empire.annotation.RdfProperty;


@MappedSuperclass
public abstract class BaseServiceSource extends BaseSource
							   implements ServiceSource
{
	 //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("pav:version")
    private String version;
    @RdfProperty("dc:publisher")
    private String publisher;
     

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

	
	protected BaseServiceSource(SourceType type) {
		super(type);
		// TODO Auto-generated constructor stub
	}
	 /**
     * Creates a new source of the specified type, identifier and
     * owning project.
     * @param  type      the {@link SourceType source type}.
     * @param  uri       the source unique identifier (URI) or
     *                   <code>null</code> if not known at this stage.
     * @param  project   the owning project or <code>null</code> if not
     *                   known at this stage.
     *
     * @throws IllegalArgumentException if any of <code>type</code>,
     *         <code>uri</code> or <code>project</code> is
     *         <code>null</code>.
     */
    protected BaseServiceSource(SourceType type, String uri, Project project) {
        super(type, uri, project);
        if (StringUtils.isBlank(uri)) {
            throw new IllegalArgumentException("uri");
        }
    }
    
  //-------------------------------------------------------------------------
  	// Methods
  	//-------------------------------------------------------------------------

    public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getPublisher() {
		return publisher;
	}
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	

   
	
	
}

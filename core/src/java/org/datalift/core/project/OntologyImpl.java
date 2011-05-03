package org.datalift.core.project;

import java.util.Date;

import javax.persistence.Entity;

import org.datalift.fwk.project.Ontology;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;


@Entity
@RdfsClass("void:vocabulary")
public class OntologyImpl extends BaseRdfEntity implements Ontology 
{
    @RdfProperty("dc:title")
    private String title;
    @RdfProperty("void:dateSubmitted")
    private Date dateSubmitted;
    @RdfProperty("dc:publisher")
    private String operator;
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public OntologyImpl() {
    	super();
    }
    
	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String getTitle() {
		return title;
	}
	
	@Override
	public Date getDateSubmitted() {
		return this.dateSubmitted;
	}

	@Override
	public void setDateSubmitted(Date dateSubmitted) {
		this.dateSubmitted = dateSubmitted;
	}

	@Override
	public String getOperator() {
		return this.operator;
	}

	@Override
	public void setOperator(String operator) {
		this.operator = operator;
	}

	@Override
	protected void setId(String id) {
	    // NOP
	}
}

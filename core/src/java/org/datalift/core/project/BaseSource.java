package org.datalift.core.project;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.datalift.fwk.project.Source;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;


@Entity
@MappedSuperclass
@RdfsClass("datalift:source")
public abstract class BaseSource extends BaseRdfEntity
    implements Source
{
    @RdfProperty("dc:title")
    private String title;

    public BaseSource() {
        super();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) braces fieldsfirst space 
// Source File Name:   BaseSource.java

package org.datalift.core.project;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.datalift.fwk.project.Source;

import com.clarkparsia.empire.annotation.Namespaces;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

// Referenced classes of package org.datalift.project:
//            BaseRdfEntity
@Entity
@MappedSuperclass
@Namespaces({"datalift", "http://www.datalift.org/core#"})
@RdfsClass("datalift:source")
public abstract class BaseSource extends BaseRdfEntity
    implements Source
{
	@RdfProperty("dc:title")
	private String title;

    public BaseSource()
    {
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }
}

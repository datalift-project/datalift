package org.datalift.fwk.project;


import java.net.URI;

import org.datalift.fwk.Module;


public interface ProjectModule extends Module
{
    public abstract URI canHandle(Project p);
}

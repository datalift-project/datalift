package org.datalift.core.project;

import java.net.URI;
import java.net.URISyntaxException;

public enum License {
    	Attribution("http://creativecommons.org/licenses/by/3.0"), 
    	Attribution_ShareAlike("http://creativecommons.org/licenses/by-sa/3.0"), 
    	Attribution_NoDerivs ("http://creativecommons.org/licenses/by-nd/3.0"),
    	Attribution_NonCommercial("http://creativecommons.org/licenses/by-nc/3.0"),
    	Attribution_NonCommercial_ShareAlike("http://creativecommons.org/licenses/by-nc-sa/3.0"),
    	Attribution_NonCommercial_NoDerivs("http://creativecommons.org/licenses/by-nc-nd/3.0");

        public final URI uri;

        License(String s) {
            try {
            	this.uri = new URI(s);
            }
            catch(URISyntaxException e)
            {
            	throw new IllegalArgumentException(e);
            }
        }
        
        public URI getUri()  {
        	return this.uri;
        }
        
    }
    
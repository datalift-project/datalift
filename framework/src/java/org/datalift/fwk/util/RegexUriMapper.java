/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project, 
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.fwk.util;


import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RegexUriMapper implements UriMapper
{
    private final Pattern extractor;
    private final MessageFormat builder;

    public RegexUriMapper(Pattern uriExtractor, MessageFormat uriBuilder) {
        if (uriExtractor == null) {
            throw new IllegalArgumentException("uriExtractor");
        }
        if (uriBuilder == null) {
            throw new IllegalArgumentException("uriBuilder");
        }
        this.extractor = uriExtractor;
        this.builder   = uriBuilder;
    }

    @Override
    public URI map(URI in) {
        URI mapped = in;
        if (in != null) {
            Matcher m = this.extractor.matcher(in.toString());
            if (m.matches()) {
                int max = m.groupCount();
                String[] args = new String[max];
                for (int i=0; i<max; i++) {
                    args[i] = m.group(i + 1);
                }
                try {
                    mapped = new URI(this.builder.format(args));
                }
                catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return mapped;
    }
}

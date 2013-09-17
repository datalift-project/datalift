/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.fwk.util;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A URI mapper than uses a {@link Pattern regular expression} to
 * select applicable URIs and extract some parts to
 * {@link Matcher#replaceAll(String) format} translated URIs.
 * URIs not matching the specified pattern are not modified (i.e.
 * returned unchanged).
 *
 * @author lbihanic
 */
public class RegexUriMapper implements UriMapper
{
    private final Pattern extractor;
    private final String replacement;

    /**
     * Creates a new URI mapper matching and extracting URI parts
     * using a {@link Pattern regular expression} and a
     * replacement string to generate the mapped URI.
     * @param  uriExtractor   the regular expression to filter the
     *                        applicable URIs and extract the parts
     *                        used to build the translated URI.
     * @param  replacement    the replacement string, compliant with
     *                        the regular expression
     *                        {@link Matcher#replaceAll(String) syntax}
     *                        for captured subsequences.
     */
    public RegexUriMapper(Pattern uriExtractor, String replacement) {
        if (uriExtractor == null) {
            throw new IllegalArgumentException("uriExtractor");
        }
        if (replacement == null) {
            throw new IllegalArgumentException("uriBuilder");
        }
        this.extractor = uriExtractor;
        this.replacement   = replacement;
    }

    /** {@inheritDoc} */
    @Override
    public URI map(URI in) {
        URI mapped = in;
        if (in != null) {
            try {
                Matcher m = this.extractor.matcher(in.toString());
                mapped = new URI(m.replaceAll(this.replacement));
            }
            catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return mapped;
    }
}

package org.datalift.fwk.util;


import java.net.URI;
import java.net.URISyntaxException;



public class PrefixUriMapper implements UriMapper
{
    private final String from;
    private final String to;

    public PrefixUriMapper(String fromPrefix, String toPrefix) {
        if (! StringUtils.isSet(fromPrefix)) {
            throw new IllegalArgumentException("fromPrefix");
        }
        if (! StringUtils.isSet(toPrefix)) {
            throw new IllegalArgumentException("toPrefix");
        }
        this.from = fromPrefix;
        this.to   = toPrefix;
    }

    @Override
    public URI map(URI in) {
        URI mapped = in;
        if (in != null) {
            String s = in.toString();
            if (s.startsWith(this.from)) {
                try {
                    mapped = new URI(s.replace(this.from, this.to));
                }
                catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return mapped;
    }
}

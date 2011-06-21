package org.datalift.core.rdf;


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

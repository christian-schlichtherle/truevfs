/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.ibm437;

import java.nio.charset.Charset;
import java.nio.charset.spi.CharsetProvider;
import java.util.*;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.annotations.ServiceImplementation;

/**
 * Provides the {@code IBM437} alias {@code CP437} character set.
 * This is the original PC character set designed for use in the USA and was
 * once implicitly assumed to be used for ZIP files before version 6.3.0 of the
 * ZIP File Format Specification officially documented it and introduced UTF-8
 * as a more advanced option - see Appendix D.
 *
 * @see    <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">ZIP File Format Specification</a>
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceImplementation(CharsetProvider.class)
public final class Ibm437CharsetProvider extends CharsetProvider {

    private static final Map<String, Charset> CHARSETS;
    static {
        final Map<String, Charset> charsets = new HashMap<>();
        for (final Charset charset : new Charset[] {
            new Ibm437Charset(),
        }) {
            charsets.put(lowerCase(charset.name()), charset);
            for (final String alias : charset.aliases())
                charsets.put(lowerCase(alias), charset);
        }
        CHARSETS = Collections.unmodifiableMap(charsets);
    }

    private static String lowerCase(String s) {
        return s.toLowerCase(Locale.ROOT);
    }

    @Override
    public Iterator<Charset> charsets() {
        return CHARSETS.values().iterator();
    }

    @Override
    public Charset charsetForName(String charset) {
        return CHARSETS.get(lowerCase(charset));
    }
}

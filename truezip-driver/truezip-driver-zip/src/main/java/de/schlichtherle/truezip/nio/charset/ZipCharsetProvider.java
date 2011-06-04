/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.nio.charset;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.charset.Charset;
import java.nio.charset.spi.CharsetProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.Immutable;

/**
 * A charset provider that only provides the {@code IBM437} character set,
 * also known as {@code CP437}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class ZipCharsetProvider extends CharsetProvider {

    private static final @NonNull Map<String, Charset> CHARSETS;
    static {
        Logger logger = Logger.getLogger(   ZipCharsetProvider.class.getName(),
                                            ZipCharsetProvider.class.getName());
        Map<String, Charset> charsets = new HashMap<String, Charset>();
        for (Charset charset : new Charset[] {
            new Ibm437Charset(),
        }) {
            charsets.put(lowerCase(charset.name()), charset);
            for (String alias : charset.aliases())
                charsets.put(lowerCase(alias), charset);
            logger.log(Level.CONFIG, "providing",
                    new Object[] { charset.displayName(), charset.aliases() });
        }
        CHARSETS = Collections.unmodifiableMap(charsets);
    }

    private static @NonNull String lowerCase(@NonNull String s) {
        return s.toLowerCase(Locale.ENGLISH);
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

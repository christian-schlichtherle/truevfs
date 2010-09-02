/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.nio.charset.IBM437Charset;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * A charset provider that only provides the {@code IBM437} character set,
 * also known as {@code CP437}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class CharsetProvider extends java.nio.charset.spi.CharsetProvider {

    private static final Collection<Charset> charsets;
    private static final Map<String, Charset> name2charset;

    static {
        charsets = Collections.unmodifiableCollection(
                Arrays.asList(new Charset[] { new IBM437Charset() }));

        name2charset = new HashMap<String, Charset>();
        for (final Charset charset : charsets) {
            name2charset.put(lowerCase(charset.name()), charset);
            for (final String alias : charset.aliases())
                name2charset.put(lowerCase(alias), charset);
        }
    }

    private static String lowerCase(String s) {
        return s.toLowerCase(Locale.ENGLISH);
    }

    public Iterator<Charset> charsets() {
        return charsets.iterator();
    }

    public Charset charsetForName(String charset) {
        return name2charset.get(lowerCase(charset));
    }
}

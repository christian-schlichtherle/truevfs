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

package de.schlichtherle.nio.charset.spi;

import de.schlichtherle.nio.charset.*;

import java.nio.charset.*;
import java.util.*;

/**
 * A charset provider that only provides the <code>IBM437</code> character set,
 * also known as <code>CP437</code>.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class CharsetProvider extends java.nio.charset.spi.CharsetProvider {
    
    private static final Map name2charset;
    private static final Collection charsets;
    
    static {
        charsets = Collections.unmodifiableCollection(
                Arrays.asList(new Charset[] { new IBM437Charset() }));
        
        name2charset = new HashMap();
        for (Iterator i = charsets.iterator(); i.hasNext(); ) {
            final Charset cs = (Charset) i.next();
            name2charset.put(lowerCase(cs.name()), cs);
            for (Iterator j = cs.aliases().iterator(); j.hasNext(); )
                name2charset.put(lowerCase((String) j.next()), cs);
        }
    }

    public Charset charsetForName(String charset) {
        return (Charset) name2charset.get(lowerCase(charset));
    }

    private static final String lowerCase(String s) {
        return s.toLowerCase(Locale.ENGLISH);
    }
    
    public Iterator charsets() {
        return charsets.iterator();
    }
}

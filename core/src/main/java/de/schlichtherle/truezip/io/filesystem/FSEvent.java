/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.filesystem;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.EventObject;
import net.jcip.annotations.Immutable;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public class FSEvent extends EventObject {

    private static final long serialVersionUID = 7656343146323435361L;

    /**
     * Constructs a new file system event.
     *
     * @param source the file system model source which caused this event.
     */
    public FSEvent(@NonNull FSModel source) {
        super(source);
    }

    /**
     * Returns the non-{@code null} file system model source which caused
     * this event.
     *
     * @return The non-{@code null} file system model source which caused
     *         this event.
     */
    @Override
    @NonNull
    public final FSModel getSource() {
        return (FSModel) source;
    }
}

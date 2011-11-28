/*
 * Copyright 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.inst.jul;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.inst.InstrumentingOutputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
class JulOutputSocket<E extends Entry>
extends InstrumentingOutputSocket<E> {

    JulOutputSocket(OutputSocket<? extends E> model, JulDirector director) {
        super(model, director);
    }

    @Override
    public final OutputStream newOutputStream() throws IOException {
        final OutputStream out = getBoundSocket().newOutputStream();
        try {
            return new JulOutputStream<E>(out, this);
        } catch (IOException ex) {
            try {
                out.close();
            } catch (IOException ex2) {
                throw (IOException) ex2.initCause(ex);
            }
            throw ex;
        }
    }
}

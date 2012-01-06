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
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDecoratingController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class InstrumentingController<M extends FsModel>
extends FsDecoratingController<M, FsController<? extends M>> {

    protected final InstrumentingDirector director;

    public InstrumentingController(
            final FsController<? extends M> controller,
            final InstrumentingDirector director) {
        super(controller);
        this.director = director.check();
    }

    @Override
    public InputSocket<?> getInputSocket(FsEntryName name, BitField<FsInputOption> options) {
        return director.instrument(delegate.getInputSocket(name, options), this);
    }

    @Override
    public OutputSocket<?> getOutputSocket(FsEntryName name, BitField<FsOutputOption> options, Entry template) {
        return director.instrument(delegate.getOutputSocket(name, options, template), this);
    }
}

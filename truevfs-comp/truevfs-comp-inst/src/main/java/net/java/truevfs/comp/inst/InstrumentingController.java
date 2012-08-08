/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsDecoratingController;
import net.java.truevfs.kernel.spec.FsNodeName;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @param  <D> the type of the director.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class InstrumentingController<D extends Director<D>>
extends FsDecoratingController {
    protected final D director;

    public InstrumentingController(
            final D director,
            final FsController controller) {
        super(controller);
        this.director = Objects.requireNonNull(director);
    }

    @Override
    public InputSocket<? extends Entry> input(
            BitField<FsAccessOption> options,
            FsNodeName name) {
        return director.instrument(this,
                controller.input(options, name));
    }

    @Override
    public OutputSocket<? extends Entry> output(
            BitField<FsAccessOption> options,
            FsNodeName name,
            @CheckForNull Entry template) {
        return director.instrument(this,
                controller.output(options, name, template));
    }
}

/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsDecoratingController;
import net.java.truevfs.kernel.spec.FsNodeName;

/**
 * @param  <M> the type of the mediator.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class InstrumentingController<M extends Mediator<M>>
extends FsDecoratingController {

    protected final M mediator;

    public InstrumentingController(
            final M mediator,
            final FsController controller) {
        super(controller);
        this.mediator = Objects.requireNonNull(mediator);
    }

    @Override
    public InputSocket<? extends Entry> input(
            BitField<FsAccessOption> options,
            FsNodeName name) {
        return mediator.instrument(this,
                controller.input(options, name));
    }

    @Override
    public OutputSocket<? extends Entry> output(
            BitField<FsAccessOption> options,
            FsNodeName name,
            @CheckForNull Entry template) {
        return mediator.instrument(this,
                controller.output(options, name, template));
    }
}

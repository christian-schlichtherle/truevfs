/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.inst;

import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.InputSocket;
import global.namespace.truevfs.comp.cio.OutputSocket;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.kernel.spec.FsAccessOption;
import global.namespace.truevfs.kernel.spec.FsController;
import global.namespace.truevfs.kernel.spec.FsDecoratingController;
import global.namespace.truevfs.kernel.spec.FsNodeName;

import java.util.Objects;
import java.util.Optional;

/**
 * @param  <M> the type of the mediator.
 * @author Christian Schlichtherle
 */
public class InstrumentingController<M extends Mediator<M>> extends FsDecoratingController {

    protected final M mediator;

    public InstrumentingController(final M mediator, final FsController controller) {
        super(controller);
        this.mediator = Objects.requireNonNull(mediator);
    }

    @Override
    public InputSocket<? extends Entry> input(
            BitField<FsAccessOption> options,
            FsNodeName name) {
        return mediator.instrument(this, getController().input(options, name));
    }

    @Override
    public OutputSocket<? extends Entry> output(
            BitField<FsAccessOption> options,
            FsNodeName name,
            Optional<? extends Entry> template) {
        return mediator.instrument(this, getController().output(options, name, template));
    }
}

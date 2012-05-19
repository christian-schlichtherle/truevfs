/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import net.truevfs.kernel.FsAccessOption;
import net.truevfs.kernel.cio.DecoratingOutputSocket;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.OutputSocket;
import net.truevfs.kernel.util.BitField;

/**
 * An output socket which provides a property for its output options.
 * 
 * @author  Christian Schlichtherle
 */
public final class OptionOutputSocket
extends DecoratingOutputSocket<Entry> {
    final BitField<FsAccessOption> options;

    public OptionOutputSocket(
            final OutputSocket<?> output,
            final BitField<FsAccessOption> options) {
        super(output);
        this.options = options;
    }

    public BitField<FsAccessOption> getOptions() {
        return options;
    }
}
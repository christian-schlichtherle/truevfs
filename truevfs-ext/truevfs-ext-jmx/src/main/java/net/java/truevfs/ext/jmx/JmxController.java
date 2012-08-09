/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Objects;
import javax.inject.Provider;
import net.java.truevfs.comp.inst.InstrumentingController;
import net.java.truevfs.kernel.spec.FsController;

/**
 * @author Christian Schlichtherle
 */
public class JmxController
extends InstrumentingController<JmxMediator>
implements JmxColleague, Provider<JmxStatistics.Kind> {
    private final JmxStatistics.Kind kind;

    JmxController(JmxMediator director, FsController controller, JmxStatistics.Kind kind) {
        super(director, controller);
        this.kind = Objects.requireNonNull(kind);
    }

    @Override
    public void start() {
    }

    @Override
    public JmxStatistics.Kind get() {
        return kind;
    }
}

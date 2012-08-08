/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import net.java.truevfs.ext.jmx.model.IoStatistics;
import net.java.truevfs.kernel.spec.FsController;

/**
 * @author Christian Schlichtherle
 */
public class JmxApplicationController extends JmxFileSystemController {

    JmxApplicationController(JmxDirector director, FsController controller) {
        super(director, controller);
    }

    @Override
    public IoStatistics getStatistics() {
        return director.getApplicationStatistics();
    }
}

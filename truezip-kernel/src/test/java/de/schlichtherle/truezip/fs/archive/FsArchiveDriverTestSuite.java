/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.socket.IOPool;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @param   <D> The type of the archive driver.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FsArchiveDriverTestSuite<D extends FsArchiveDriver<?>>
extends FsArchiveDriverTestBase<D> {

    private static final Logger
            logger = Logger.getLogger(FsArchiveDriverTestSuite.class.getName());

    private static final int NUM_ENTRIES = 10;

    @Before
    public void setUp() throws IOException {
        super.setUp();
    }

    @Test
    public void allArchiveDriversMustBeFederated() {
        assert getArchiveDriver().isFederated();
    }

    @Test
    public void itsIOPoolMustNotBeNull() {
        assertNotNull(getArchiveDriver().getPool());
    }

    @Test
    public void itsIOPoolShouldBeConstant() {
        final IOPool<?> p1 = getArchiveDriver().getPool();
        final IOPool<?> p2 = getArchiveDriver().getPool();
        if (p1 != p2)
            logger.log(Level.WARNING, "{0} returns different I/O buffer pools upon multiple invocations of getPool()!", getArchiveDriver().getClass());
    }

    @Test(expected = NullPointerException.class)
    public void newControllerMustNotTolerateNullModel() {
        getArchiveDriver().newController(null, newDummyController(newArchiveModel()));
    }

    @Test(expected = NullPointerException.class)
    public void newControllerMustNotTolerateNullParent() {
        getArchiveDriver().newController(newArchiveModel(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void newControllerMustCheckArchiveModel() {
        getArchiveDriver().newController(newNonArchiveModel(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void newControllerMustCheckParentMemberMatch() {
        final FsModel model = newArchiveModel();
        getArchiveDriver().newController(model, newDummyController(model));
    }

    @Test
    public void newControllerMustNotReturnNull() {
        final FsModel model = newArchiveModel();
        assertNotNull(getArchiveDriver().newController(
                model,
                newDummyController(model.getParent())));
    }

    @Test
    public void newControllerMustMeetPostConditions() {
        final FsModel model = newArchiveModel();
        final FsController<?> parent = newDummyController(model.getParent());
        final FsController<?> controller = getArchiveDriver().newController(
                model, parent);
        assertNotNull(controller);
        assertEquals(model.getMountPoint(), controller.getModel().getMountPoint());
        assertSame(parent, controller.getParent());
    }

    private static <M extends FsModel> FsController<M>
    newDummyController(final M model) {
        final FsModel pm = model.getParent();
        final FsController<FsModel> pc = null == pm
                ? null
                : newDummyController(pm);
        return new DummyController<M>(model, pc);
    }

    private static FsModel newArchiveModel() {
        final FsModel parent = newNonArchiveModel();
        return new FsDefaultModel(
                FsMountPoint.create(URI.create(
                    "archive:" + parent.getMountPoint() + "path!/")),
                newNonArchiveModel());
    }

    private static FsModel newNonArchiveModel() {
        return new FsDefaultModel(
                FsMountPoint.create(URI.create("file:/")),
                null);
    }

    
}

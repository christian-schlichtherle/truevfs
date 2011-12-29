/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.archive.FsArchiveDriver;
import de.schlichtherle.truezip.util.SuffixSet;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import org.junit.After;
import org.junit.Before;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public abstract class TestBase<D extends FsArchiveDriver<?>> {

    protected static final FsMountPoint
            ROOT_DIRECTORY = FsMountPoint.create(URI.create("file:/"));
    protected static final FsMountPoint
            CURRENT_DIRECTORY = FsMountPoint.create(new File("").toURI());
    protected static final String[] NO_STRINGS = new String[0];
    protected final int
            NUM_THREADS = 10 * Runtime.getRuntime().availableProcessors();
    private static final String ARCHIVE_DETECTOR = "archiveDetector";

    private @Nullable D driver;
    private @Nullable TArchiveDetector detector;
    private @Nullable Map<String, ?> environment;

    protected abstract String getSuffixList();

    protected FsScheme getScheme() {
        return FsScheme.create(new SuffixSet(getSuffixList()).iterator().next());
    }

    protected final String getSuffix() {
        return "." + getScheme();
    }

    protected abstract D newArchiveDriver();

    protected final @Nullable D getArchiveDriver() {
        return driver;
    }

    protected final @Nullable TArchiveDetector getArchiveDetector() {
        return detector;
    }

    protected final @Nullable Map<String, ?> getEnvironment() {
        return environment;
    }

    @Before
    public void setUp() throws Exception {
        final D driver = newArchiveDriver();
        final TArchiveDetector detector = new TArchiveDetector(
                getSuffixList(), driver);
        final Map<String, Object> environment = new HashMap<String, Object>();
        environment.put(ARCHIVE_DETECTOR, detector);
        final TConfig config = TConfig.push();
        config.setLenient(true);
        config.setArchiveDetector(detector);
        this.driver = driver;
        this.detector = detector;
        this.environment = environment;
    }

    @After
    public void tearDown() throws Exception {
        TConfig.pop();
    }

    protected final void runParallel(
            final TaskFactory factory,
            final int nThreads)
    throws InterruptedException, ExecutionException {
        final Collection<Callable<Void>> tasks
                = new ArrayList<Callable<Void>>(nThreads);
        for (int i = 0; i < nThreads; i++)
            tasks.add(factory.newTask(i));

        final List<Future<Void>> results;
        final ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        try {
            results = executor.invokeAll(tasks);
        } finally {
            executor.shutdown();
        }

        for (final Future<Void> result : results)
            result.get(); // check exception from task
    }

    @SuppressWarnings("ProtectedInnerClass")
    protected interface TaskFactory {
        Callable<Void> newTask(int threadNum);
    }
}

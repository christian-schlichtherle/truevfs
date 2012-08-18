/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access.exp;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;

/**
 * An immutable aggregate of configuration options for accessing the virtual
 * file system space.
 * Transformation methods are provided to update instances of this class with
 * a given option by creating a new instance.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class TConfig {

    /**
     * The default configuration results from scanning the class path for a
     * file system manager and file system drivers.
     * Its access preferences will be set to
     * {@code BitField.of(FsAccessOption.CREATE_PARENTS)}.
     */
    public static final TConfig DEFAULT = new TConfig(
            FsManagerLocator.SINGLETON.get(),
            TArchiveDetector.ALL,
            BitField.of(FsAccessOption.CREATE_PARENTS));

    private final FsManager manager;
    private final TArchiveDetector detector;
    private final BitField<FsAccessOption> preferences;

    TConfig(final FsManager manager,
            final TArchiveDetector detector,
            final BitField<FsAccessOption> preferences) {
        this.manager = Objects.requireNonNull(manager);
        this.detector = Objects.requireNonNull(detector);
        this.preferences = Objects.requireNonNull(preferences);
    }

    /**
     * Returns the file system manager of this configuration.
     * 
     * @return The file system manager of this configuration.
     */
    FsManager getManager() { return manager; }

    /**
     * Returns the archive detector of this configuration.
     * 
     * @return The archive detector of this configuration.
     */
    public TArchiveDetector getDetector() { return detector; }

    /**
     * Returns a configuration which results from updating this configuration
     * with the given {@code detector}.
     * 
     * @param  detector the archive detector to use in the returned
     *         configuration.
     * @return A configuration which results from updating this configuration
     *         with the given {@code detector}.
     */
    public TConfig detector(final TArchiveDetector detector) {
        return detector.equals(this.detector)
                ? this
                : new TConfig(manager, detector, preferences);
    }

    /**
     * Returns the access preferences of this configuration.
     * 
     * @return The access preferences of this configuration.
     */
    public BitField<FsAccessOption> getPreferences() { return preferences; }

    /**
     * Returns a configuration which results from updating this configuration
     * with the given {@code preferences}.
     * 
     * @param  preferences the access preferences to use in the returned
     *         configuration.
     * @return A configuration which results from updating this configuration
     *         with the given {@code preferences}.
     */
    public TConfig preferences(final BitField<FsAccessOption> preferences) {
        return preferences.equals(this.preferences)
                ? this
                : new TConfig(manager, detector, preferences);
    }

    public TFile newFile(String path) { return new TFile(this, path); }
}

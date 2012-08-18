/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access.exp;

import java.util.NoSuchElementException;
import net.java.truevfs.access.TArchiveDetector;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class TConfigsTest {

    @Test
    public void dependencyInjection1() {
// START SNIPPET: dependencyInjection1
        final TConfig original = TConfig.DEFAULT;
        final TConfig config = original.detector(TArchiveDetector.NULL);
        assert !config.equals(original);
        final TFile file = config.newFile("archive.zip/entry");
        assert file.getConfig() == config;
// END SNIPPET: dependencyInjection1
    }

    @Ignore("Breaks parallel test execution!")
    @Test
    public void notThreadSafeSideEffect1() {
// START SNIPPET: notThreadSafeSideEffect1
        final TConfig original = TConfigs.get();
        assert original == TConfig.DEFAULT;
        final TConfig config = original.detector(TArchiveDetector.NULL);
        assert !config.equals(original);
        TConfigs.set(config);
        assert TConfigs.get() == config;
        final TFile file = new TFile("archive.zip/entry");
        assert file.getConfig() == config;
// END SNIPPET: notThreadSafeSideEffect1
    }

    @Test
    public void threadSafeSideEffect1() {
// START SNIPPET: threadSafeSideEffect1
        final TConfig original = TConfigs.get();
        assert original == TConfig.DEFAULT;
        final TConfig push = original.detector(TArchiveDetector.NULL);
        assert !push.equals(original);
        TConfigs.push(push);
        try {
            assert TConfigs.get() == push;
            final TFile file = new TFile("archive.zip/entry");
            assert file.getConfig() == push;
        } finally {
            final TConfig pop = TConfigs.pop();
            assert pop == push;
        }
        assert TConfigs.get() == original;
        try {
            TConfigs.pop();
            fail();
        } catch (NoSuchElementException expected) {
        }
// END SNIPPET: threadSafeSideEffect1
    }

    @Test
    public void threadSafeSideEffect2() {
// START SNIPPET: threadSafeSideEffect2
        final TConfig original = TConfigs.get();
        assert original == TConfig.DEFAULT;
        TConfig push1 = original.detector(TArchiveDetector.NULL);
        assert !push1.equals(original);
        TConfigs.push(push1);
        try {
            assert TConfigs.get() == push1;
            final TConfig push2 = push1.detector(TArchiveDetector.ALL);
            assert !push2.equals(push1);
            TConfigs.push(push2);
            try {
                assert TConfigs.get() == push2;
                final TFile file = new TFile("archive.zip/entry");
                assert file.getConfig() == push2;
            } finally {
                final TConfig pop2 = TConfigs.pop();
                assert pop2 == push2;
            }
            assert TConfigs.get() == push1;
        } finally {
            TConfig pop1 = TConfigs.pop();
            assert pop1 == push1;
        }
        assert TConfigs.get() == original;
        try {
            TConfigs.pop();
            fail();
        } catch (NoSuchElementException expected) {
        }
// END SNIPPET: threadSafeSideEffect2
    }
}

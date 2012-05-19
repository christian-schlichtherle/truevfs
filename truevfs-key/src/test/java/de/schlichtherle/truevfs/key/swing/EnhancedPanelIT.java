/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.key.swing;

import de.schlichtherle.truevfs.key.swing.EnhancedPanel;
import de.schlichtherle.truevfs.key.swing.EnhancedPanel;
import de.schlichtherle.truevfs.key.swing.EnhancedPanel;
import de.schlichtherle.truevfs.key.swing.PanelEvent;
import de.schlichtherle.truevfs.key.swing.PanelEvent;
import de.schlichtherle.truevfs.key.swing.PanelEvent;
import de.schlichtherle.truevfs.key.swing.PanelListener;
import de.schlichtherle.truevfs.key.swing.PanelListener;
import de.schlichtherle.truevfs.key.swing.PanelListener;
import java.awt.EventQueue;
import java.awt.Window;
import java.util.EventListener;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JFrameOperator;

/**
 * @author  Christian Schlichtherle
 */
public final class EnhancedPanelIT extends JemmyUtils {
    private EnhancedPanel instance;

    @Before
    public void setUp() {
        instance = new EnhancedPanel();
        instance.add(new JLabel("Hello world!"));
    }

    @Test
    public void testGetAncestorWindow() throws InterruptedException {
        assertNull(instance.getAncestorWindow());

        JFrameOperator frame = showFrameWith(instance);
        Window window1 = instance.getAncestorWindow();
        assertSame(frame.getSource(), window1);
        frame.dispose();

        frame = showFrameWith(instance); // change enclosing frame
        Window window2 = instance.getAncestorWindow();
        assertSame(frame.getSource(), window2);
        frame.dispose();

        assertNotSame(window1, window2);
    }

    @Test
    public void testPanelListeners0() {
        PanelListener listener;
        EventListener[] listeners;

        listeners = instance.getPanelListeners();
        assertEquals(0, listeners.length);

        listener = null;

        try {
            instance.addPanelListener(listener);
            fail();
        } catch (NullPointerException expected) {
        }
        listeners = instance.getPanelListeners();
        assertEquals(0, listeners.length);

        try {
            instance.removePanelListener(listener);
            fail();
        } catch (NullPointerException expected) {
        }
        listeners = instance.getPanelListeners();
        assertEquals(0, listeners.length);

        listener = new CountingPanelListener();

        instance.addPanelListener(listener);
        listeners = instance.getPanelListeners();
        assertEquals(1, listeners.length);
        assertSame(listener, listeners[0]);

        instance.addPanelListener(listener);
        listeners = instance.getPanelListeners();
        assertEquals(2, listeners.length);
        assertSame(listener, listeners[0]);
        assertSame(listener, listeners[1]);

        instance.removePanelListener(listener);
        listeners = instance.getPanelListeners();
        assertEquals(1, listeners.length);
        assertSame(listener, listeners[0]);

        instance.removePanelListener(listener);
        listeners = instance.getPanelListeners();
        assertEquals(0, listeners.length);
    }

    @Test
    public void testPanelListeners1() {
        PanelListener listener;
        EventListener[] listeners;

        listeners = instance.getListeners(PanelListener.class);
        assertEquals(0, listeners.length);

        listener = null;

        try {
            instance.addPanelListener(listener);
            fail();
        } catch (NullPointerException expected) {
        }
        listeners = instance.getListeners(PanelListener.class);
        assertEquals(0, listeners.length);

        try {
            instance.removePanelListener(listener);
            fail();
        } catch (NullPointerException expected) {
        }
        listeners = instance.getListeners(PanelListener.class);
        assertEquals(0, listeners.length);

        listener = new CountingPanelListener();

        instance.addPanelListener(listener);
        listeners = instance.getListeners(PanelListener.class);
        assertEquals(1, listeners.length);
        assertSame(listener, listeners[0]);

        instance.addPanelListener(listener);
        listeners = instance.getListeners(PanelListener.class);
        assertEquals(2, listeners.length);
        assertSame(listener, listeners[0]);
        assertSame(listener, listeners[1]);

        instance.removePanelListener(listener);
        listeners = instance.getListeners(PanelListener.class);
        assertEquals(1, listeners.length);
        assertSame(listener, listeners[0]);

        instance.removePanelListener(listener);
        listeners = instance.getListeners(PanelListener.class);
        assertEquals(0, listeners.length);
    }

    @Test
    public void testFireAncestorWindowShown() {
        final CountingPanelListener l = new CountingPanelListener();
        instance.addPanelListener(l);
        instance.addPanelListener(l); // add again to receive same event twice!
        assertEquals(0, l.shown);

        PanelEvent event = null;
        instance.fireAncestorWindowShown(event);
        assertEquals(2, l.shown);
    }

    @Test
    public void testFireAncestorWindowHidden() {
        final CountingPanelListener l = new CountingPanelListener();
        instance.addPanelListener(l);
        instance.addPanelListener(l); // add again to receive same event twice!
        assertEquals(0, l.hidden);

        PanelEvent event = null;
        instance.fireAncestorWindowHidden(event);
        assertEquals(2, l.hidden);
    }

    /**
     * Adds the EnhancedPanel instance to a frame, shows and hides this frame
     * multiple times and counts these events.
     * Then puts the same EnhancedPanel instance into a second frame and repeats
     * the test.
     */
    @Test
    public void testEvents4JFrame() throws Exception {
        final CountingPanelListener l = new CountingPanelListener();
        instance.addPanelListener(l);

        class FrameHolder {
            protected final JFrame frame;

            protected FrameHolder(JFrame frame) {
                this.frame = frame;
            }
        }

        class SetupFrame extends FrameHolder implements Runnable {
            SetupFrame(JFrame frame) {
                super(frame);
            }

            @Override
            public void run() {
                frame.add(instance);
                frame.pack();
                frame.setLocationRelativeTo(null);
            }
        }

        class MakeFrameVisible extends FrameHolder implements Runnable {
            MakeFrameVisible(JFrame frame) {
                super(frame);
            }

            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                // Use multiple ways to make the frame visible.
                // Depending on the version of the Swing implementation, this may
                // create multiple PanelEvents with an ID equal to
                // PanelEvent.ANCESTOR_WINDOW_SHOWN.
                // However, all these events should get coalesced into one.
                frame.show();
                frame.setVisible(true);
            }
        }

        class MakeFrameInvisible extends FrameHolder implements Runnable {
            MakeFrameInvisible(JFrame frame) {
                super(frame);
            }

            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                // Use multiple ways to make the frame invisible.
                // Depending on the version of the Swing implementation, this may
                // create multiple PanelEvents with an ID equal to
                // PanelEvent.ANCESTOR_WINDOW_HIDDEN.
                // However, all these events should get coalesced into one.
                frame.setVisible(false);
                frame.hide();
                frame.dispose();
            }
        }

        // Test in first frame.
        final JFrame frame1 = new JFrame();
        EventQueue.invokeAndWait(new SetupFrame(frame1));
        events(new MakeFrameVisible(frame1), new MakeFrameInvisible(frame1), l);

        // Test in second frame.
        final JFrame frame2 = new JFrame();
        EventQueue.invokeAndWait(new SetupFrame(frame2));
        events(new MakeFrameVisible(frame2), new MakeFrameInvisible(frame2), l);
    }

    private void events(
            final Runnable makeVisible,
            final Runnable makeInvisible,
            final CountingPanelListener l)
    throws  Exception {
        l.shown = l.hidden = 0; // reset counters

        for (int i = 1; i <= 3; i++) {
            EventQueue.invokeAndWait(makeVisible);
            assertEquals(i, l.shown);
            assertEquals(i - 1, l.hidden);

            EventQueue.invokeAndWait(makeInvisible);
            assertEquals(i, l.shown);
            assertEquals(i, l.hidden);
        }
    }

    /**
     * Shows and hides the EnhancedPanel instance several times in a
     * JOptionPane and counts these events.
     */
    @Test
    public void testEvents4JOptionPane() throws Exception {
        final CountingPanelListener l = new CountingPanelListener();
        instance.addPanelListener(l);
        final String title = EnhancedPanelIT.class.getSimpleName();
        final Runnable makeVisible = new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(
                        null, instance, title, JOptionPane.INFORMATION_MESSAGE);
            }
        };
        for (int i = 1; i <= 3; i++) {
            runOnEdt(makeVisible);
            final JDialogOperator dialog = new JDialogOperator(title); // wait for JOptionPane
            assertEquals(i, l.shown);
            assertEquals(i - 1, l.hidden);

            final JButtonOperator button = new JButtonOperator(dialog);
            button.push();
            button.getQueueTool().waitEmpty(WAIT_EMPTY);
            assertEquals(i, l.shown);
            assertEquals(i, l.hidden);
        }
    }

    private static class CountingPanelListener implements PanelListener {
        public int shown, hidden;

        @Override
        public void ancestorWindowShown(PanelEvent evt) {
            shown++;
        }

        @Override
        public void ancestorWindowHidden(PanelEvent evt) {
            hidden++;
        }
    }
}
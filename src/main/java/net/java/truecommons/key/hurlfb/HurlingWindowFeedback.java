/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.hurlfb;

import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.java.truecommons.key.spec.util.SuspensionPenalty;
import net.java.truecommons.key.swing.feedback.DecoratingFeedback;
import net.java.truecommons.key.swing.feedback.Feedback;

/**
 * Provides feedback by hurling the containing window for
 * {@link SuspensionPenalty#MIN_KEY_RETRY_DELAY} / 2 milliseconds.
 * <p>
 * This class is inspired by chapter #38 "Earthquake Dialog" of the book
 * "Swing Hacks" by Joshua Marinacci & Chris Adamson, published by O'Reilly
 * in 2005.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
final class HurlingWindowFeedback
extends DecoratingFeedback implements Feedback {

    private static final double PI      = Math.PI;
    private static final double TWO_PI  = 2.0 * PI;

    private static final double amplitude = 25;
    private static final double cycle = 150;
    private static final int    duration = SuspensionPenalty.MIN_KEY_RETRY_DELAY / 2;
    private static final int    fps = 75;

    HurlingWindowFeedback(Feedback feedback) {
        super(feedback);
    }

    @Override
    public void run(JPanel panel) {
        feedback.run(panel); // temporarily disable default button
        final Window window = SwingUtilities.getWindowAncestor(panel);
        if (null == window) return;
        final Point origin = window.getLocation();
        final long start = System.currentTimeMillis();
        final Timer timer = new Timer(1000 / fps, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final long elapsed = System.currentTimeMillis() - start;
                if (elapsed < duration && window.isShowing()) {
                    final double angle = TWO_PI * elapsed / cycle;
                    final double offset
                            = Math.sin(PI * elapsed / duration) * amplitude;
                    final int x = (int) (Math.cos(angle) * offset + origin.x);
                    final int y = (int) (Math.sin(angle) * offset + origin.y);
                    window.setLocation(x, y);
                    window.repaint();
                } else {
                    ((Timer) e.getSource()).stop();
                    window.setLocation(origin);
                    window.repaint();
                }
            }
        });
        timer.start();
    }
}

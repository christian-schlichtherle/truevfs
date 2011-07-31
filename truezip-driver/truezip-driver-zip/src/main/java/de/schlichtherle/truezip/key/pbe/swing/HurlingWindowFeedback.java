/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.key.pbe.swing;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import static de.schlichtherle.truezip.key.SafeKeyProvider.*;

/**
 * Provides run by beeping using the default toolkit, disabling the
 * default button in the root pane for three seconds and concurrently
 * hurling the containing window for 1.5 seconds.
 * <p>
 * This class is inspired by chapter #38 "Earthquake Dialog" of the book
 * "Swing Hacks" by Joshua Marinacci & Chris Adamson, published by O'Reilly
 * in 2005.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class HurlingWindowFeedback extends BasicInvalidKeyFeedback {

    private static final double PI      = Math.PI;
    private static final double TWO_PI  = 2.0 * PI;
    
    public static final int AMPLITUDE = 25;
    public static final int CYCLE     = 150;
    public static final int DURATION  = 1500;
    public static final int FPS       = 75;

    private final double amplitude;
    private final double cycle;
    private final int    duration;
    private final int    fps;
    
    public HurlingWindowFeedback() {
        this(AMPLITUDE, CYCLE, DURATION, FPS);
    }

    /**
     * Constructs a new {@code HurlingWindowFeedback}.
     * 
     * @param amplitude the amplitude of pixels for offsetting the window.
     * @param cycle milliseconds required for one cycle.
     * @param duration millisecons of duration of quake.
     * @param fps frames per second for animation.
     */
    protected HurlingWindowFeedback(    final int amplitude,
                                        final int cycle,
                                        final int duration,
                                        final int fps) {
        super(duration > MIN_KEY_RETRY_DELAY ? duration : MIN_KEY_RETRY_DELAY);
        this.amplitude = amplitude;
        this.cycle     = cycle;
        this.duration  = duration;
        this.fps       = fps;
    }

    @Override
    public void run(@NonNull JPanel panel) {
        final Window window = SwingUtilities.getWindowAncestor(panel);
        super.run(panel); // temporarily disable default button
        if (null == window)
            return;
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

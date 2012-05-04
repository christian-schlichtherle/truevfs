/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key.pbe.swing;

import de.schlichtherle.truezip.key.swing.EnhancedPanel;
import de.schlichtherle.truezip.key.swing.PanelEvent;
import de.schlichtherle.truezip.key.swing.PanelListener;
import de.truezip.key.param.SafePbeParameters;
import de.schlichtherle.truezip.key.pbe.swing.feedback.Feedback;
import java.net.URI;
import javax.annotation.CheckForNull;

/**
 * Abstract panel for prompting for authentication keys.
 *
 * @author Christian Schlichtherle
 */
abstract class KeyPanel extends EnhancedPanel {

    private static final long serialVersionUID = 2762934728646652873L;

    private Feedback feedback;

    KeyPanel() {
        addPanelListener(new KeyPanelListener());
    }

    /**
     * Returns the feedback to run when this panel is shown in its ancestor
     * window.
     */
    public Feedback getFeedback() {
        return feedback;
    }

    /**
     * Sets the feedback to run when this panel is shown in its ancestor
     * window.
     */
    public void setFeedback(final Feedback feedback) {
        this.feedback = feedback;
    }

    private void runFeedback() {
        final Feedback feedback = getFeedback();
        if (null != feedback)
            feedback.run(this);
    }

    /**
     * Getter for property {@code resource}.
     *
     * @return Value of property {@code resource}.
     */
    public abstract URI getResource();

    /**
     * Setter for property {@code resource}.
     *
     * @param resource New value of property {@code resource}.
     */
    public abstract void setResource(final URI resource);

    /**
     * Getter for property {@code error}.
     */
    public abstract @CheckForNull String getError();

    /**
     * Setter for property error.
     *
     * @param error New value of property error.
     */
    public abstract void setError(final @CheckForNull String error);

    abstract boolean updateParam(final SafePbeParameters<?, ?> param);

    private static class KeyPanelListener implements PanelListener {
        @Override
        public void ancestorWindowShown(final PanelEvent evt) {
            ((KeyPanel) evt.getSource()).runFeedback();
        }

        @Override
        public void ancestorWindowHidden(PanelEvent evt) {
        }
    }
}
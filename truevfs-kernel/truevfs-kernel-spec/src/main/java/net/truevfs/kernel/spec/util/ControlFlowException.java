/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.truevfs.kernel.spec.util;

import javax.annotation.concurrent.Immutable;

/**
 * Indicates a condition which requires non-local control flow.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("serial") // serializing control flow exceptions is nonsense!
public class ControlFlowException extends RuntimeException {

    private static final String TRACEABLE_PROPERTY_KEY
            = ControlFlowException.class.getName() + ".traceable";

    public ControlFlowException() { this(null); }

    public ControlFlowException(final Throwable cause) {
        super(null != cause ? cause.toString() : null, cause, false, isTraceable());
    }

    /**
     * Returns {@code true} if and only if a control flow exception should have
     * a full stack trace instead of an empty stack trace.
     * If and only if the system property with the key string
     * {@code net.truevfs.kernel.spec.util.ControlFlowException.traceable}
     * is set to {@code true} (whereby case is ignored), then instances of this
     * class will have a regular stack trace, otherwise their stack trace will
     * be empty.
     * Note that this should be set to {@code true} for debugging purposes only.
     * 
     * @return {@code true} if and only if a control flow exception should have
     *         a full stack trace instead of an empty stack trace.
     */
    public static boolean isTraceable() {
        return Boolean.getBoolean(TRACEABLE_PROPERTY_KEY);
    }
}

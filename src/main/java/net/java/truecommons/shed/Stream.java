/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.shed;

/**
 * A generic stream collection interface.
 * Mind you that you need to call {@link #close()} on this stream after use,
 * best with a {@code try}-with-resources statement.
 * For example, let's assume you would have an object named {@code container}
 * which has a method named {@code stream()} which returns a
 * {@code Stream<Object>}.
 * Then you should use this method like this:
 * <p>
 * <pre>{@code
 * try (Stream<Object> stream = container.stream()) {
 *     for (Object object : stream) {
 *         // Use object here...
 *     }
 * }
 * }</pre>
 * The {@code try}-with-resources statement ensures that {@code stream} gets
 * {@code close()}d after the {@code for}-loop, even if it terminates with an
 * exception.
 * 
 * @param  <T> The type of the elements in the stream.
 * @since  TrueCommons 1.0.12
 * @author Christian Schlichtherle
 */
public interface Stream<T> extends Iterable<T>, AutoCloseable {

    /**
     * Closes this stream.
     * It is an error to call any other method on this stream once this method
     * has terminated without an exception and the result of any violation is
     * undefined.
     * However, implementations are encouraged to throw an exception from any
     * other method to indicate this error condition.
     */
    @Override
    void close() throws Exception;
}

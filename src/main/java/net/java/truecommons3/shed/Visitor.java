/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

/**
 * A generic visitor for items of any type.
 *
 * @param  <I> The type of items to {@link #visit}.
 * @param  <X> The type of exceptions to be thrown by {@link #visit}.
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
public interface Visitor<I, X extends Exception> {

    /**
     * Visits the given item.
     *
     * @param  item the item to visit.
     * @throws Exception at the discretion of the implementation.
     */
    void visit(I item) throws X;
}

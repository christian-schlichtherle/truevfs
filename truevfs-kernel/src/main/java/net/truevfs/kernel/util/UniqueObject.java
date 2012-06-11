/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util;

/**
 * A unique object {@linkplain #equals(Object) compares equal} only to itself
 * and has a consistent {@linkplain #hashCode() hash code}.
 * The purpose of this abstract class is to stay DRY when a sub class
 * <em>cannot</em> reasonably have an {@code equals} or {@code hashCode}
 * implementation which differs from the default implementation in
 * {@link Object}.
 * 
 * @author Christian Schlichtherle
 */
public class UniqueObject {

    /**
     * Objects of this type always only compare equal to themselve.
     * 
     * @param that an object.
     * @return {@code this == that}
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public final boolean equals(Object that) { return this == that; }

    /**
     * Objects of this type always return a distinct integer for disting
     * objects.
     * 
     * @return The same integer as {@link Object#hashCode()}.
     */
    @Override
    public final int hashCode() { return super.hashCode(); }
}

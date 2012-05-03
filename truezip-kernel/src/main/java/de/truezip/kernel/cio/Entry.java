/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import static de.truezip.kernel.cio.Entry.Type.*;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Represents an entry in a container.
 * <p>
 * In general, if a property has an unknown value, its getter method must
 * return the value {@link #UNKNOWN} or {@code null} respectively.
 * <p>
 * Some constants of this interface are unmodifiable sets of enums.
 * These are convenient to use for loops like this:
 * <pre><code>
 * for (Type type : ALL_TYPE_SET)
 *     ...;
 * </code></pre>
 *
 * @author Christian Schlichtherle
 */
public interface Entry {

    /** The unknown value for numeric properties, which is {@value}. */
    byte UNKNOWN = -1;

    /**
     * Returns the <i>entry name</i>.
     * When parsed, an entry name is interpreted as follows:
     * <ol>
     * <li>An entry name is a sequence of <i>segments</i> which are
     *     separated by one or more <i>separator characters</i>, which may be
     *     {@code '/'} or {@code '\\'} or both, according to the rules of the
     *     {@linkplain Container}.
     *     This implies that a segment cannot contain separator characters.
     * <li>An entry name may contain one or more dot ({@code "."}) or
     *     dot-dot ({@code ".."}) segments which represent the current or
     *     parent segment respectively.
     * <li>An entry name may start with one or more separator characters.
     *     In this case, its said to be <i>absolute</i>.
     *     Otherwise, its said to be <i>relative</i>.
     * <li>An entry name may end with one or more separator characters
     *     (e.g. to identify a directory entry).
     * </ol>
     * For example, the entry names {@code "foo/bar/"} and
     * {@code "./abc/../foo/./def/./../bar/."} both refer to the same entry
     * when being parsed.
     *
     * @return The entry name.
     */
    String getName();

    /** Defines the type of entry. */
    @SuppressWarnings("PublicInnerClass")
    enum Type {

        /**
         * Regular file.
         * A file usually has some content associated to it which can be read
         * and written using a stream.
         */
        FILE,

        /**
         * Regular directory.
         * A directory can have other file system entries as members.
         */
        DIRECTORY,

        /**
         * Symbolic (named) link.
         * A symbolic link refers to another file system entry which could even
         * be located outside the current file system.
         */
        SYMLINK,

        /**
         * Special file.
         * A special file is a byte or block oriented interface to an arbitrary
         * I/O device, e.g. a hard disk or a network service.
         */
        SPECIAL,
    }

    /** An unmodifiable set of just {@link Type#FILE}. */
    Set<Type> FILE_TYPE_SET = Collections.unmodifiableSet(EnumSet.of(FILE));
    /** An unmodifiable set of just {@link Type#DIRECTORY}. */
    Set<Type> DIRECTORY_TYPE_SET = Collections.unmodifiableSet(EnumSet.of(DIRECTORY));
    /** An unmodifiable set of just {@link Type#SYMLINK}. */
    Set<Type> SYMLINK_TYPE_SET = Collections.unmodifiableSet(EnumSet.of(SYMLINK));
    /** An unmodifiable set of just {@link Type#SPECIAL}. */
    Set<Type> SPECIAL_TYPE_SET = Collections.unmodifiableSet(EnumSet.of(SPECIAL));
    /** An unmodifiable set of all enums in {@link Type}. */
    Set<Type> ALL_TYPE_SET = Collections.unmodifiableSet(EnumSet.allOf(Type.class));

    /** Defines the type of size information for an entry. */
    @SuppressWarnings("PublicInnerClass")
    enum Size {
        DATA,
        STORAGE,
    }

    /** An unmodifiable set of all enums in {@link Size}. */
    Set<Size> ALL_SIZE_SET = Collections.unmodifiableSet(EnumSet.allOf(Size.class));

    /**
     * Returns the size of the given {@code type} for this entry in bytes or
     * {@link #UNKNOWN} if not specified or the type is unsupported.
     * This method may not be meaningful for non-{@link Type#FILE} entries.
     *
     * @param  type the type of the size.
     * @return The size of the given {@code type} for this entry in bytes or
     *         {@link #UNKNOWN} if not specified or the type is unsupported.
     */
    long getSize(Size type);

    /** Defines the type of access information for an entry. */
    @SuppressWarnings("PublicInnerClass")
    enum Access {
        CREATE,
        READ,
        WRITE,
        EXECUTE,
    }

    /** An unmodifiable set of all enums in {@link Access}. */
    Set<Access> ALL_ACCESS_SET = Collections.unmodifiableSet(EnumSet.allOf(Access.class));

    /**
     * Returns the time of the given access {@code type} for this entry in
     * milliseconds since the epoch or {@value #UNKNOWN} if not
     * specified or the type is unsupported.
     *
     * @param  type the type of the access.
     * @return The time of the given access {@code type} for this entry in
     *         milliseconds since the epoch or {@value #UNKNOWN} if not
     *         specified or the type is unsupported.
     */
    long getTime(Access type);

    /** Defines access entities. */
    @SuppressWarnings({"MarkerInterface", "PublicInnerClass"})
    interface Entity {
    } // Entity

    /** Defines access entities for POSIX systems. */
    @SuppressWarnings("PublicInnerClass")
    enum PosixEntity implements Entity {
        USER,
        GROUP,
        OTHER,
    } // Entity

    /** An unmodifiable set of all enums in {@link PosixEntity}. */
    Set<PosixEntity> ALL_POSIX_ENTITY_SET = Collections.unmodifiableSet(EnumSet.allOf(PosixEntity.class));

    /**
     * Returns {@code true} if and only if the given access {@code type} to
     * this entry is permitted for the given {@code entity}.
     * 
     * @param  type the type of the access.
     * @param  entity the entity which desires access.
     * @return {@code true} if and only if the given access {@code type} to
     *         this entry is permitted for the given {@code entity}.
     */
    Boolean isPermitted(Access type, Entity entity);    
}

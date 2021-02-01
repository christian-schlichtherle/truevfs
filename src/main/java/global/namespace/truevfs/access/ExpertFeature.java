/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access;

import global.namespace.truevfs.kernel.api.FsSyncOption;
import global.namespace.truevfs.kernel.api.FsSyncOptions;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import java.util.Locale;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Indicates a feature which requires a certain experience level for safe use.
 * In most cases, using an expert feature is a code smell and you should
 * refactor your code to avoid it in order to stay safe.
 * However, some valid use cases exist for an expert feature
 * (otherwise it should have been {@code @Deprecated}).
 * These use cases are not explained here because a true expert will know them!
 * <p>
 * As of TrueVFS 0.10, using an expert feature is not checked.
 * However, future versions of TrueVFS might include tools to issue a warning
 * whenever an expert feature is unintentionally used - similar to the way
 * {@code javac} treats the {@code @Deprecated} annotation.
 *
 * @author Christian Schlichtherle
 */
@Documented
@Inherited
@Target({ CONSTRUCTOR, METHOD })
public @interface ExpertFeature {

    /** Returns the experience level required to safely use this feature. */
    Level level() default Level.MASTER;

    /** Returns the reasons why this feature should only be used by an expert. */
    Reason[] value();

    /**
     * Returns a warning message for future use, for example in static code
     * analysis.
     */
    String warning() default "Using this %s requires %s experience because %s!";

    /** The experience level required to safely use an expert feature. */
    enum Level {
        INTERMEDIATE, MASTER;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT).replace('_', ' ');
        }
    }

    /** The reason why a feature should only be used by an expert. */
    enum Reason {

        /**
         * Injecting a different archive detector for the same virtual file
         * system path may cause data corruption.
         * This is because the behavior of the virtual file system is not only
         * a pure function of the operation parameters, but also depends on the
         * {@linkplain TConfig#current() current configuration}, the state
         * of the virtual file system and ultimately the state of the
         * underlying host or network file system.
         * <p>
         * The state of the virtual file system very much depends on the
         * archive detector which has been previously used to access the same
         * virtual file system path.
         * If there is a mismatch between the injected archive detector and the
         * previously used archive detector, then the result of an operation
         * is hard to predict but you can expect to see spurious
         * <strong>false positive</strong> or
         * <strong>false negative</strong> archive file detection or even
         * <strong>data corruption</strong> up to the point of
         * <strong>total data loss</strong>!
         * <p>
         * This is particularly true for multithreaded applications: If a
         * thread changes the archive detector which is associated with a given
         * virtual file system path, then another thread may get an unexpected
         * result when accessing the same virtual file system path, with all
         * the after effects described above.
         * <p>
         * Now that you have been warned, here is a high-level strategy for
         * effectively associating a different archive detector to a given
         * virtual file system path:
         * <ol>
         * <li>Lock out any other thread from concurrently accessing the
         *     virtual file system path.
         * <li>Totally clear the state associated with the virtual file system
         *     path in the Kernel by calling the method {@link TVFS#umount()}.
         *     Your best choice is the umount method with no parameters.
         *     However, if you have to do this selectively, make sure the
         *     filter parameter really covers the virtual file system path you
         *     want to access and the effective options are
         *     {@link FsSyncOptions#UMOUNT}.
         * <li>Now access the virtual file system path with the new archive
         *     detector.
         *     Depending on the particular archive detector, this will trigger
         *     mounting the archive file system by the Kernel.
         *     The new archive detector is then associated with the given file
         *     system path.
         * <li>Depending on which operation you have performed and whether the
         *     change of the archive detector shall be persistent or not, you
         *     may need to repeat the second step.
         * <li>Unlock access to the virtual file system path for other threads
         *     to see the result.
         *     Mind you that these threads may still be using the old archive
         *     detector.
         * </ol>
         * <p>
         * I leave it up to you to figure the details - again, this is an
         * expert feature!
         *
         * @see <a href="http://truezip.java.net/truezip-file/usage.html#Third_Party_Access">Third Party Access</a>
         */
        INJECTING_A_DIFFERENT_DETECTOR_FOR_THE_SAME_PATH_MAY_CORRUPT_DATA,

        /**
         * The presence or absence of some synchronization options may yield
         * unwanted side effects.
         * For example, if the {@link FsSyncOption#CLEAR_CACHE} is absent, then
         * the selective entry cache doesn' get cleared and hence the state
         * associated with a particular file system path may not get totally
         * cleared.
         * On the other hand, the presence of the
         * {@link FsSyncOption#FORCE_CLOSE_IO} will forcibly close any open
         * streams and channels (after some timeout), regardless if they are
         * currently used by other threads or not.
         */
        THE_PRESENCE_OR_ABSENCE_OF_SOME_OPTIONS_MAY_YIELD_UNWANTED_SIDE_EFFECTS;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT).replace('_', ' ');
        }
    }
}

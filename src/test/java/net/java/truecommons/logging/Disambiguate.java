package net.java.truecommons.logging;

import org.slf4j.Logger;

/** @author Christian Schlichtherle */
class Disambiguate {

    private Disambiguate() { }

    static void trace2(Logger logger, String format, Object arg1, Object arg2) {
        logger.trace(format, arg1, arg2);
    }

    static void debug2(Logger logger, String format, Object arg1, Object arg2) {
        logger.debug(format, arg1, arg2);
    }

    static void info2(Logger logger, String format, Object arg1, Object arg2) {
        logger.info(format, arg1, arg2);
    }

    static void warn2(Logger logger, String format, Object arg1, Object arg2) {
        logger.warn(format, arg1, arg2);
    }

    static void error2(Logger logger, String format, Object arg1, Object arg2) {
        logger.error(format, arg1, arg2);
    }
}

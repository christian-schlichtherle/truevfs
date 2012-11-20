#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.java;

import net.java.truevfs.access.TApplication;
import net.java.truevfs.kernel.spec.FsSyncException;

/**
 * An abstract class which configures the TrueVFS Access module.
 * 
 * @author Christian Schlichtherle
 */
public abstract class Application<E extends Exception> extends TApplication<E> {

    /**
     * Overridden to make the application wait until it gets interrupted.
     * Uncomment this method if you want to test the JMX interface with the
     * short living sample applications of this archetype.
     */
    /*@Override
    protected void sync() throws FsSyncException {
        System.out.println("Waiting until interrupt...");
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ex) {
        }
        super.sync();
    }*/
}

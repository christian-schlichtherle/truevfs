#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.scala

import net.java.truevfs.access.TApplication

/**
 * An abstract class which configures the TrueVFS Access module.
 * 
 * @author Christian Schlichtherle
 */
abstract class Application extends TApplication {

    /**
      * Overridden to make the application wait until it gets interrupted.
      * Uncomment this method if you want to test the JMX interface with the
      * short living sample applications of this archetype.
      */
    /*override protected def sync() {
        System.err.println("Waiting until interrupt...");
        try {
          Thread.sleep(Long.MaxValue)
        } catch {
          case ex: InterruptedException =>
        }
        super.sync();
    }*/
}

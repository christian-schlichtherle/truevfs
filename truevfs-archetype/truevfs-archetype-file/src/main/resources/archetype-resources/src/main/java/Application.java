#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import de.schlichtherle.truevfs.key.pbe.swing.feedback.HurlingWindowFeedback;
import de.schlichtherle.truevfs.key.pbe.swing.feedback.InvalidKeyFeedback;
import net.truevfs.file.TApplication;

/**
 * An abstract class which configures the TrueVFS Access File* module.
 * 
 * @author  Christian Schlichtherle
 */
abstract class Application<E extends Exception> extends TApplication<E> {

    /**
     * Runs the setup phase.
     * <p>
     * This method is {@link #run run} only once at the start of the life
     * cycle.
     * Its task is to configure the default behavior of the TrueVFS Access File* API
     * in order to answer the following questions:
     * <ul>
     * <li>What are the file extensions which shall get recognized as archive
     *     files and hence as virtual directories?
     * <li>Shall missing archive files and directory entries get automatically
     *     created whenever required?
     * </ul>
     * <p>
     * The implementation in the class {@link Application} configures
     * the type of the feedback when prompting the user for keys for RAES
     * encrypted ZIP alias ZIP.RAES alias TZP files by the Swing based
     * prompting key manager.
     * If this JVM is running in headless mode, then this configuration is
     * ignored and the user is prompted by the console I/O based prompting
     * key manager.
     */
    @Override
    protected void setup() {
        String spec = InvalidKeyFeedback.class.getName();
        String impl = HurlingWindowFeedback.class.getName();
        System.setProperty(spec, System.getProperty(spec, impl));
    }

    /*@Override
    protected void sync() throws FsSyncException {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ex) {
            Logger  .getLogger(Application.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        super.sync();
    }*/
}

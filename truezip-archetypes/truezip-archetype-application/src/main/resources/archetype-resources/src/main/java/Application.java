#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

//import de.schlichtherle.truezip.file.TDefaultArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsSyncException;
//import de.schlichtherle.truezip.fs.archive.tar.TarBZip2Driver;
//import de.schlichtherle.truezip.fs.archive.tar.TarDriver;
//import de.schlichtherle.truezip.fs.archive.tar.TarGZipDriver;
//import de.schlichtherle.truezip.fs.archive.zip.ZipDriver;
//import de.schlichtherle.truezip.socket.sl.IOPoolLocator;

/**
 * An abstract base class which runs the setup-work-sync lifecycle for a
 * typical TrueZIP application.
 * Subclass or edit this template class to meet your requirements.
 * <p>
 * The typical lifecycle of a TrueZIP application consists of the
 * {@link #setup()} phase and one or more iterations of the {@link #work()}
 * and {@link #sync()} phases.
 * Thus, this lifecycle may simply get referenced as the
 * <i>setup-work-sync lifecycle</i>.
 * 
 * @param <E> the {@link Exception} class to throw by {@link #work} and thus
 *       {@link #run}, too.
 * @author Christian Schlichtherle
 */
abstract class Application<E extends Exception> {

    /**
     * Runs the setup-work-sync lifecycle.
     * At first, this method calls {@link #setup()}.
     * Then, a loop is entered which calls {@link #work()} and (in a finally
     * block) {@link #sync()}.
     * If {@link #work()} returns a negative integer, then the loop is
     * repeated.
     * Otherwise, the return value is used as the
     * {@link System#exit(int) exit status} of the VM.
     * <p>
     * Note that calling {@link #sync()} in a finally-block ensures that all
     * unsynchronized changes to the contents of all federated file systems
     * (i.e. archive files) get committed to their respective parent file
     * system, even if {@link #work()} throws an exception.
     * 
     * @throws Exception At the discretion of the {@link #work()} method.
     * @throws FsSyncException At the discretion of the {@link #sync()} method.
     */
    protected final int run(String[] args) throws E, FsSyncException {
        setup();
        int status;
        do {
            try {
                status = work(args);
            } finally {
                sync();
            }
        } while (status < 0);
        return status;
    }

    /**
     * Runs the setup phase.
     * <p>
     * This method is {@link #run() run} only once at the start of the life
     * cycle.
     * Its task is to configure the default behaviour of the File* API
     * in order to answer the following questions:
     * <ul>
     * <li>What are the file suffixes which shall be recognized as archive
     *     files and hence as virtual directories?
     * <li>Shall missing archive files and directory entries get automatically
     *     created whenever required?
     * </ul>
     */
    protected void setup() {
        // The constructors of the TFile class use the ArchiveDetector
        // interface to scan a path name for suffixes of archive files
        // which shall be treated like virtual directories.
        // You can either explicitly inject this ArchiveDetector dependency
        // into a TFile constructor or you can rely on the value of the class
        // property "defaultArchiveDetector" as follows:
        //   If you comment out the following statement,
        // all TFile objects will use a default ArchiveDetector which
        // recognizes the canonical archive file suffixes registered by all
        // archive driver modules which are present on the class path at run
        // time - which can be configured by editing the pom.xml file.
        //   However, if you uncomment the following statement,
        // all TFile objects will use the given default ArchiveDetector which
        // recognizes only the given canonical file suffixes for TAR, TAR.GZ,
        // TAR.BZ2 and ZIP files as archive files and hence as virtual
        // directories.
        // This requires the JARs for the archive driver modules
        // truezip-driver-tar and truezip-driver-zip to be present on the
        // class path at compile time:
        /*TFile.setDefaultArchiveDetector(
                new TDefaultArchiveDetector(
                    TDefaultArchiveDetector.NULL,
                    new Object[][] {
                        { "tar", new TarDriver(IOPoolLocator.SINGLETON) },
                        { "tgz|tar.gz", new TarGZipDriver(IOPoolLocator.SINGLETON) },
                        { "tbz|tb2|tar.bz2", new TarBZip2Driver(IOPoolLocator.SINGLETON) },
                        { "zip", new ZipDriver(IOPoolLocator.SINGLETON)},
                    }));*/
        
        // This class property controls whether archive files and their member
        // directories get automatically created whenever required.
        // By default, the value of this class property is {@code true}!
        //TFile.setLenient(false);
    }

    /**
     * Runs the work phase.
     * <p>
     * This method is {@link #run() run} at least once and repeatedly called
     * until it returns a non-negative integer for use as the
     * {@link System#exit(int) exist status} of the VM.
     * <p>
     * After calling this method, the {@link #sync()} method is run in a
     * finally-block.
     * <p>
     * BTW: Avoid repeating this method and updating the same archive file upon
     * each call:
     * This would degrade the overall performance from O(n) to O(m*n),
     * where m is the number of new or modified entries and n is the number
     * of all entries in the archive file!
     * 
     * @return A negative integer in order to continue calling this method
     *         in a loop.
     *         Otherwise, the return value is used as the
     *         {@link System#exit(int) exit status} of the VM.
     */
    protected abstract int work(String[] args) throws E;

    /**
     * Runs the sync phase.
     * <p>
     * This method is {@link #run() run} in a finally-block after each call to
     * the {@link #work()} method.
     * <p>
     * Its task is to commit all unsynchronized changes to the contents of all
     * federated file systems (i.e. archive files) to their respective parent
     * file system.
     * This will also clean up the cache with its temporary files and thereby
     * allow third parties to safely read, update or delete these archive files
     * before they are accessed by the File* API again.
     * <p>
     * BTW: In case this method does nothing, there is a JVM shutdown hook to
     * performs the same tasks as the {@link TFile#umount()} method.
     */
    protected void sync() throws FsSyncException {
        TFile.umount();
    }
}

import java.io.*;
import java.util.*;

public class FileTest extends Thread {

    final long start = System.currentTimeMillis();
    long cf = 0, df = 0, cd = 0, dd = 0;

    public static void main(String[] args) {
        // Don't forget to set the System property java.io.tmpdir!
        //new FileTest().start(); // doesn't make a difference
        new FileTest().run();
    }

    public void run() {
        try {
            runIO();
        } catch (IOException failure) {
            failure.printStackTrace();
        }
    }

    public void runIO() throws IOException {
        byte[] data = new byte[10]; // keep small for high error rates
        new Random().nextBytes(data);
        while (true) {
            File file = File.createTempFile("tmp", null);
            for (long i = 1;; i++) {
                try {
                    OutputStream out = new FileOutputStream(file);
                    try {
                        out.write(data);
                    } finally {
                        out.close();
                    }
                } catch (IOException failure) {
                    cf++;
                    failure.printStackTrace();
                    printStats(i);
                    break;
                }
                if (!file.delete()) {
                    df++;
                    printStats(i);
                    break;
                }
                if (!file.mkdir()) {
                    cd++;
                    printStats(i);
                    break;
                }
                if (!file.delete()) {
                    dd++;
                    printStats(i);
                    break;
                }
            }
        }
    }

    void printStats(long i) {
        System.err.println("Minutes elapsed: " + (System.currentTimeMillis() - start) / (60 * 1000));
        System.err.println("Failure in run #" + i);
        System.err.println("Total failures to create file: " + cf);
        System.err.println("Total failures to delete file: " + df);
        System.err.println("Total failures to create directory: " + cd);
        System.err.println("Total failures to delete directory: " + dd);
    }
}

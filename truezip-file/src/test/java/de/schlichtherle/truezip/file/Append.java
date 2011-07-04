/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.schlichtherle.truezip.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Feasibility test for appending to an archive file while reading from it.
 * FIXME: Evolve this to a full blown integration test in the
 * {@link TFileTestSuite}.
 * Mind that this might only work with certain file system drivers.
 * For example, the TZP driver will not be able to use this because it's
 * impossible to append to an enrypted file without recomputing the MAC from
 * the entire previous clear text content - otherwise a MAC would be useless.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class Append {
    @Test
    public void testAppend() throws IOException {
        byte[] one = new byte[1024];
        new Random().nextBytes(one);
        File tmp = File.createTempFile("tzp", null);
        try {
            {
                OutputStream out = new FileOutputStream(tmp);
                try {
                    out.write(one);
                } finally {
                    out.close();
                }
            }
            RandomAccessFile raf = new RandomAccessFile(tmp, "r");
            try {
                OutputStream out = new FileOutputStream(tmp, true);
                try {
                    byte[] two = new byte[1024];
                    new Random().nextBytes(two);
                    System.out.println(tmp + ": length: " + raf.length());
                    out.write(two);
                    raf.readFully(two);
                    assertTrue(Arrays.equals(one, two));
                    System.out.println(tmp + ": length: " + raf.length());
                } finally {
                    out.close();
                }
            } finally {
                raf.close();
            }
        } finally {
            tmp.delete();
        }
    }
}

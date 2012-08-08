/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.truevfs.comp.zip.driver;

import net.java.truevfs.comp.zip.DateTimeConverter;
import net.java.truevfs.comp.zip.ZipEntry;

/**
 * ZIP driver entries apply the date/time conversion rules as defined by
 * {@link DateTimeConverter#ZIP}.
 *
 * @see    #getDateTimeConverter()
 * @see    ZipDriver
 * @author Christian Schlichtherle
 */
public class ZipDriverEntry extends AbstractZipDriverEntry {

    public ZipDriverEntry(String name) {
        super(name);
    }

    protected ZipDriverEntry(String name, ZipEntry template) {
        super(name, template);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipDriverEntry} returns
     * {@link DateTimeConverter#ZIP}.
     *
     * @return {@link DateTimeConverter#ZIP}
     */
    @Override
    protected DateTimeConverter getDateTimeConverter() {
        return DateTimeConverter.ZIP;
    }
}

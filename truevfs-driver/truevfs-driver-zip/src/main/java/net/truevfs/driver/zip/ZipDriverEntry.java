/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.truevfs.driver.zip;

import net.truevfs.driver.zip.core.AbstractZipDriverEntry;
import net.truevfs.driver.zip.core.io.DateTimeConverter;
import net.truevfs.driver.zip.core.io.ZipEntry;

/**
 *
 * @author christian
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

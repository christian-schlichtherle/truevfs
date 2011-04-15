#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ${package};

import de.schlichtherle.truezip.file.TFile;
import java.io.IOException;

/**
 * Recursively copies the first file or directory argument to the second file
 * or directory argument.
 * Instead of a directory, you can name any supported archive file type in the
 * path names, too.
 * If you name any archive files in the destination path name, they get
 * automatically created.
 * E.g. if the destination path name is {@code archive.zip} it gets created as
 * a ZIP file unless it already exists.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Copy {
    public static void main(String[] args) throws IOException {
        TFile src = new TFile(args[0]);
        TFile dst = new TFile(args[1]);
        if (dst.isArchive() || dst.isDirectory())
            dst = new TFile(dst, src.getName());
        TFile.cp_rp(src, dst);
    }
}

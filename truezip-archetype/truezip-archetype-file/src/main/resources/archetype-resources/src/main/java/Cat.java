#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ${package};

import de.schlichtherle.truezip.file.TFile;
import java.io.IOException;

/**
 * This command line utility concatenates the contents of the parameter paths
 * on the standard output.
 * 
 * @see     <a href="http://www.gnu.org/software/wget/">GNU Cat - Home Page</a>
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class Cat extends Application<IOException> {

    public static void main(String[] args) throws IOException {
        System.exit(new Cat().run(args));
    }

    @Override
    protected int work(String[] args) throws IOException {
        for (String arg : args)
            new TFile(arg).output(System.out);
        return 0;
    }
}

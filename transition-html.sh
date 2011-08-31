find -name '*.html' | while read file; do
    gawk -- '\
/^  \- Licensed under the Apache License/, /^  \- limitations under the License/ {\
    if (!DONE) {\
        DONE = 1;\
        print "  - All rights reserved. This program and the accompanying materials";\
        print "  - are made available under the terms of the Eclipse Public License v1.0";\
        print "  - which accompanies this distribution, and is available at";\
        print "  - http://www.eclipse.org/legal/epl-v10.html";\
    }\
    next;\
}\
{ print; }\
' "$file" > file && cp file "$file"
done

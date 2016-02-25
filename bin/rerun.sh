#!/bin/sh

PDFJS="../pdf.js-versions/pdf.js@iesl"

HERE=$(pwd)

rm ./src/main/js/pdf.combined.js

cd $PDFJS

node make singlefile

cp build/singlefile/build/pdf.combined.js  $HERE/src/main/js/

# cd $HERE

# for f in samples-mit/*.pdf;
# do
#     bin/run.js -i $f -o $f.svg --svg > $f.err.txt
#     cat $f.svg | xmlstarlet fo > $f.1.svg
#     mv $f.1.svg $f.svg

# done

# rm 2839err.txt
# bin/run.js -i samples-mit/2839.pdf -o 2839x.svg --svg > 2839err.txt
# cat 2839x.svg | xmlstarlet fo > 2839.svg
# rm 2839x.svg

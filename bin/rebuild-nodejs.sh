#!/bin/sh

PDFJS="../pdf.js-versions/pdf.js@iesl"

HERE=$(pwd)

cd $PDFJS

node make singlefile
rm ../../iesl-pdf-to-text/src/main/js/pdf.combined.js
cp build/singlefile/build/pdf.combined.js  ../../iesl-pdf-to-text/src/main/js/


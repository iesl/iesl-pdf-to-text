#!/usr/bin/env node

//
// Basic node example that prints document metadata and text content.
// Requires single file built version of PDF.js -- please run
// `node make singlefile` before running the example.
//

var fs = require('fs');

// HACK few hacks to let PDF.js be loaded not as a module in global space.
global.window = global;
global.navigator = { userAgent: "node" };
global.PDFJS = {};
global.DOMParser = require('./lib/node/domparsermock.js').DOMParserMock;



require('./lib/pdf.combined.js');
require('./lib/node/domstubs.js');




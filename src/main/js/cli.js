if (typeof define !== 'function') {
    var define = require('amdefine')(module);
}

define(function(require) {

    var options = require('commander');
    var fs = require('fs');

    options
        .version('1.0')
        .option('--meta', 'Extract raw metadata list (debugging)')
        .option('--svg', 'Emit SVG; requires --input and --output options')
        .option('--svgd', 'Emit SVG, one per PDF page')
        .option('--html', 'Emit HTML')
        .option('-i, --input [input-pdf]', 'pdf input filename')
        .option('-o, --output [output-svg]', 'svg output filename')
        .parse(process.argv);

    // Loading file from file system into typed array
    var pdfPath = options.input;
    var data = new Uint8Array(fs.readFileSync(pdfPath));

    var p2x; 
    if (options.meta) {
        p2x = require('./pdfToMetalist');
        p2x.render(options.input, data);
    } else if (options.svg) {
        p2x = require('./pdfToSvg');
        p2x.render(options.input, options.output, data);
    } else if (options.svgd) {
        p2x = require('./pdfToSvgJsDom');
        p2x.render(options.input, data);
    } else if (options.html) {
        p2x = require('./pdfToHtml');
        p2x.render(options.input, data);
    } else {
        options.help();
    }

});


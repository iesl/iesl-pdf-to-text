if (typeof define !== 'function') {
    var define = require('amdefine')(module);
}

define(function(require) {

    var options = require('commander');
    var fs = require('fs');

    options
        .version('1.0')
        .option('--svg', 'Emit SVG; requires input and output')
        .option('-i, --input [input-pdf]', 'pdf input filename')
        .option('-o, --output [output-svg]', 'svg output filename')
        .option('-f, --embedfont', 'flag to keep all font information in svg')
        .parse(process.argv);

    // Loading file from file system into typed array
    var data = new Uint8Array(fs.readFileSync(options.input));

    var p2x;
    if (options.meta) {
    } else if (options.svg) {
        p2x = require('./pdfToSvg');
        p2x.render(options.output, data, options['embedfont'] || false);
    } else {
        options.help();
    }

});

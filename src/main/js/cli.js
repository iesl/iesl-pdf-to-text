if (typeof define !== 'function') {
    var define = require('amdefine')(module);
}

define(function(require) {

    var options = require('commander');
    var fs = require('fs');

    options
        .version('1.0')
        .option('--meta', 'Extract raw metadata list (debugging)')
        .option('--svg', 'Emit SVG, one per PDF page')
        .option('--svgd', 'Emit SVG, one per PDF page')
        .option('--html', 'Emit HTML')
        .option('-f, --file [pdf]', 'pdf input filename')
        .parse(process.argv);

    if (options.file == undefined) {
        options.help();
    }
    
    // Loading file from file system into typed array
    var pdfPath = options.file;
    var data = new Uint8Array(fs.readFileSync(pdfPath));

    var p2x; 
    if (options.meta) {
        p2x = require('./pdfToMetalist');
        p2x.render(options.file, data);
    } else if (options.svg) {
        p2x = require('./pdfToSvg');
        p2x.render(options.file, data);
    } else if (options.svgd) {
        p2x = require('./pdfToSvgJsDom');
        p2x.render(options.file, data);
    } else if (options.html) {
        p2x = require('./pdfToHtml');
        p2x.render(options.file, data);
    } else {
        options.help();
    }

});


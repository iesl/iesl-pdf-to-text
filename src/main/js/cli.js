if (typeof define !== 'function') {
  var define = require('amdefine')(module);
}

define(function(require) {

  var options = require('commander');

  options
    .version('1.0')
    .option('-i, --input [input-pdf]', 'pdf input filename')
    .option('-o, --output [output-svg]', 'svg output filename')
    .option('-p, --pages', 'output one svg per pdf page')
    .option('-f, --embedfont', 'flag to keep all font information in svg')
    .option('-h, --help []', 'print help')
    .parse(process.argv);

  // console.log(options);

  var fs = require('fs');
  // Loading file from file system into typed array
  var data = new Uint8Array(fs.readFileSync(options.input));

  var p2x = require('./pdfToSvg');
  p2x.render(options.output, data, options['embedfont'] || false);

});

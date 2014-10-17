if (typeof define !== 'function') {
  var define = require('amdefine')(module);
}

define(function(require) {

  var options = require('commander');
  var fs = require('fs');

  options
    .version('1.0')
    .option('--meta', 'Extract raw metadata list')
    .option('--svg', 'Extract raw metadata list')
    .option('--html', 'Extract htmlized text')
    .option('-f, --file [pdf]', 'pdf input filename')
    .parse(process.argv);
  
  // Loading file from file system into typed array
  var pdfPath = options.file;
  var data = new Uint8Array(fs.readFileSync(pdfPath));

  if (options.meta) {
    var p2x = require('./pdfToMetalist');
    p2x.render(options.file, data);
  } else if (options.svg) {
    var p2x = require('./pdfToSvg');
    p2x.render(options.file, data);
  } else if (options.html) {
    var p2x = require('./pdfToHtml');
    p2x.render(options.file, data);
  }

});


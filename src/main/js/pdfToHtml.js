if (typeof define !== 'function') {
  var define = require('amdefine')(module);
}

define(function(require) {
  var utils = require('./utils');
  // var dep = require('dependency');
  // HACK few hacks to let PDF.js be loaded not as a module in global space.
  //global.window = global;
  //global.navigator = { userAgent: "node" };
  //global.PDFJS = {};
  //global.DOMParser = require('./node/domparsermock.js').DOMParserMock;

  var scale = 1.0; //Set this to whatever you want. This is basically the "zoom" factor for the PDF.

  //// Run some jQuery on a html fragment
  //var jsdom = require("jsdom");
  // 
  //jsdom.env(
  //  '<p><a class="the-link" href="https://github.com/tmpvar/jsdom">jsdom!</a></p>',
  //  [],
  //  function (errors, window) {
  //    global.window = global;
  //    global.PDFJS = {};
  //    global.DOMParser = require('./lib/node/domparsermock.js').DOMParserMock;
  // 
  //    renderPdfToSVG();
  //    //renderPdfToMetadata();
  // 
  //  }
  //);


  var _ = require('underscore');

  function renderPdfToSvgPlus(filename, data) {
    // Create and append the 'pdf-page' div to the pdf container.
    var jsdom = require('jsdom').jsdom;
    var document = jsdom("hello world");
    var window = document.parentWindow;

    global = window;
    // global.window = global;
    global.navigator = { userAgent: "node" };
    global.PDFJS = {};
    //// window.PDFJS = global.PDFJS;
    global.DOMParser = require('./node/domparsermock.js').DOMParserMock;

    require('./svg-plus');

    // Will be using promises to load document, pages and misc data instead of
    // callback.
    PDFJS.getDocument(data).then(function (doc) {
      var numPages = doc.numPages;
      console.log('# Document Loaded');
      console.log('Number of Pages: ' + numPages);
      console.log();

      var lastPromise = Promise.resolve(); // will be used to chain promises
      var loadPage = function (pageNum) {
        return doc.getPage(pageNum).then(function (page) {
          console.log('# Page ' + pageNum);
          var viewport = page.getViewport(1.0 /* scale */);
          console.log('Size: ' + viewport.width + 'x' + viewport.height);
          console.log();
          
          return page.getOperatorList().then(function (opList) {
            var svgGfx = new PDFJS.SVGGraphics(page.commonObjs, page.objs);
            svgGfx.embedFonts = true;
            return svgGfx.getSVG(opList, viewport).then(function (svg) {
              var svgDump = svg.toString();
              var outputFilename = utils.addExt('svg',
                                                utils.addExt(('p'+pageNum),
                                                             utils.stripExt(
                                                               utils.getFileNameFromPath(filename))));
              utils.writeToFile('./svgout/', outputFilename, svgDump);
            });
          });
        })
      };
      
      for (var i = 1; i <= numPages; i++) {
        lastPromise = lastPromise.then(loadPage.bind(null, i));
      }
      return lastPromise;
    }).then(function () {
      console.log('# End of Document');
    }, function (err) {
      console.error('Error: ' + err);
    });
  }  


  return {
    render: renderPdfToSvgPlus
  };

});



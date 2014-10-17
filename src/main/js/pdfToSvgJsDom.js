if (typeof define !== 'function') {
  var define = require('amdefine')(module);
}

define(function(require) {

  // Dumps svg outputs to a folder called svgdump
  function writeToFile(svgdump, pageNum) {
    var name = getFileNameFromPath(pdfPath);
    fs.mkdir('./svgdump/', function(err) {
      if (!err || err.code === 'EEXIST') {
        fs.writeFile('./svgdump/' + name + "-" + pageNum + '.svg', svgdump,
                     function(err) {
                       if (err) {
                         console.log('Error: ' + err);
                       } else {
                         console.log('Page: ' + pageNum);
                       }
                     });
      }
    });
  }

  // Get filename from the path

  function getFileNameFromPath(path) {
    var index = path.lastIndexOf('/');
    var extIndex = path.lastIndexOf('.');
    return path.substring(index , extIndex);
  }

  function renderPdfToSVG(data) {

    //require('./node/domstubs.js');

    var jsdom = require('jsdom');

    global.PDFJS = {};
    
    jsdom.env('<p></p>', [
      './pdf.combined.js'
    ], function (errors, window) {
      //global.window = global;
      //global.navigator = { userAgent: "node" };
      //global.PDFJS = {};
      //global.DOMParser = require('./node/domparsermock.js').DOMParserMock;
      //window.PDFJS = {};

      require('./pdf.combined.js');
             
      //global.DOMParser = require('./node/domparsermock.js').DOMParserMock;
      // var parentWindow = jsdom().parentWindow;
      
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
              svgGfx.embedFonts = false;
              return svgGfx.getSVG(opList, viewport).then(function (svg) {
                var svgDump = svg.toString();
                writeToFile(svgDump, pageNum);
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

      // free memory associated with the window
      window.close();
    });

  }

  return {
      render: renderPdfToSVG
  };

});


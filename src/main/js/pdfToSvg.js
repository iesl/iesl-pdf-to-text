if (typeof define !== 'function') {
  var define = require('amdefine')(module);
}

define(function(require) {

  var fs = require('fs');
  
  // Dumps svg outputs to a folder called svgdump
  function writeToFile(svgdump, pageNum, pdfPath) {
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

  // HACK few hacks to let PDF.js be loaded not as a module in global space.
  global.window = global;
  global.navigator = { userAgent: "node" };
  global.PDFJS = {};
  global.DOMParser = require('./node/domparsermock.js').DOMParserMock;



  require('./pdf.combined.js');
  require('./node/domstubs.js');
  
  function renderPdfToSVG(filename, data) {

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
              writeToFile(svgDump, pageNum, filename);
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
    render: renderPdfToSVG
  };

});


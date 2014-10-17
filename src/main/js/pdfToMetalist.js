if (typeof define !== 'function') {
  var define = require('amdefine')(module);
}

define(function(require) {
  // var dep = require('dependency');
  // HACK few hacks to let PDF.js be loaded not as a module in global space.
  global.window = global;
  global.navigator = { userAgent: "node" };
  global.PDFJS = {};
  global.DOMParser = require('./node/domparsermock.js').DOMParserMock;

  require('./pdf.combined.js');
  require('./node/domstubs.js');

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

    function renderPage(page) {
      var viewport = page.getViewport(scale);

      // Create and append the 'pdf-page' div to the pdf container.
      //var pdfPage = document.createElementNS('div');
      //pdfPage.className = 'pdfPage';
      //var pdfContainer = document.getElementById('pdfContainer');
      // pdfContainer.appendChild(pdfPage);

      // Set the canvas height and width to the height and width of the viewport.
      var canvas = document.createElementNS('canvas');
      var context = canvas.getContext('2d');

      // The following few lines of code set up scaling on the context, if we are
      // on a HiDPI display.
      var outputScale = getOutputScale(context);
      canvas.width = (Math.floor(viewport.width) * outputScale.sx) | 0;
      canvas.height = (Math.floor(viewport.height) * outputScale.sy) | 0;
      context._scaleX = outputScale.sx;
      context._scaleY = outputScale.sy;
      if (outputScale.scaled) {
        context.scale(outputScale.sx, outputScale.sy);
      }

      // The page, canvas and text layer elements will have the same size.
      canvas.style.width = Math.floor(viewport.width) + 'px';
      canvas.style.height = Math.floor(viewport.height) + 'px';

      pdfPage.style.width = canvas.style.width;
      pdfPage.style.height = canvas.style.height;
      pdfPage.appendChild(canvas);

      var textLayerDiv = document.createElementNS('div');
      textLayerDiv.className = 'textLayer';
      textLayerDiv.style.width = canvas.style.width;
      textLayerDiv.style.height = canvas.style.height;
      pdfPage.appendChild(textLayerDiv);

      // Painting the canvas...
      var renderContext = {
        canvasContext: context,
        viewport: viewport
      };
      var renderTask = page.render(renderContext);

      // ... and at the same time, getting the text and creating the text layer.
      var textLayerPromise = page.getTextContent().then(function (textContent) {
        var textLayerBuilder = new TextLayerBuilder({
          textLayerDiv: textLayerDiv,
          viewport: viewport,
          pageIndex: 0
        });
        textLayerBuilder.setTextContent(textContent);
      });

      // We might be interested when rendering complete and text layer is built.
      return Promise.all([renderTask.promise, textLayerPromise]);
    }

    function renderPdfToMetadata(data) {
      // Will be using promises to load document, pages and misc data instead of
      // callback.
      PDFJS.getDocument(data).then(function (doc) {
        var numPages = doc.numPages;
        console.log('# Document Loaded');
        console.log('Number of Pages: ' + numPages);
        console.log();


        var lastPromise; // will be used to chain promises
        lastPromise = doc.getMetadata().then(function (data) {
          console.log('# Metadata Is Loaded');
          console.log('## Info');
          console.log(JSON.stringify(data.info, null, 2));
          console.log();
          if (data.metadata) {
            console.log('## Metadata');
            console.log(JSON.stringify(data.metadata.metadata, null, 2));
            console.log();
          }
        });

        var loadPage = function (pageNum) {
          return doc.getPage(pageNum).then(function (page) {
            console.log('# Page ' + pageNum);
            //console.log(renderPage(page));
            // console.log(page.getOperatorList());
            
            console.log(page)
            var viewport = page.getViewport(1.0 /* scale */);
            console.log('Size: ' + viewport.width + 'x' + viewport.height);
            console.log();
            return page.getTextContent().then(function (content) {
              // Content contains lots of information about the text layout and
              // styles, but we need only strings at the moment
              var strings = content.items.map(function (item) {
                return item.str;
              });
              console.log('## Text Content');
              console.log(strings.join(' '));
              console.log('## Obj Content');
              var objContent = content.items.map(function (item) {
                console.log(item);
              });
              
            }).then(function () {
              console.log();
            });
          })
        };
        // Loading of the first page will wait on metadata and subsequent loadings
        // will wait on the previous pages.
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
      render: renderPdfToMetadata
    };

});



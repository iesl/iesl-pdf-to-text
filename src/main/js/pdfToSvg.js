if (typeof define !== 'function') {
  var define = require('amdefine')(module);
}

define(function(require) {

  var fs = require('fs');
  var _ = require('underscore');

  // Dumps svg outputs to a folder called svgdump
  function writeToFile(svgdump, svgPath) {

    console.log("writing...", svgPath);
    fs.writeFile(svgPath, svgdump, function(err) {
      if (err) {
        console.log('Error: ' + err);
      } else {
        console.log('Name: ' + getFileNameFromPath(svgPath));
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

  require('./node/domstubs.js');

  PDFJS.workerSrc = true;
  require('./pdf.combined.js');


  var xmlserializer = require('xmlserializer');
  var Image = require('canvas').Image;

  function renderPdfToSVG(outputPath, data, keepAllFonts) {

    var jsdom = require('jsdom');

    var PRJ_ROOT = process.cwd();

    var root = ('<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" '+
                ' "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd"><svg:svg '+
                'xmlns:xlink="http://www.w3.org/1999/xlink" '+
                'xmlns:svg="http://www.w3.org/2000/svg" />');


    jsdom.env(root, [], function (errors, window) {

      global.Image = Image;
      global.document = window.document;

      PDFJS.getDocument(data).then(function (doc) {
        var numPages = doc.numPages;
        var svgBookPromise = _.foldl(_.range(1, numPages + 1), function(svgBookAccPro, pageNum) {

          return Promise.resolve(svgBookAccPro.then(function(svgBookAcc) {

            return Promise.resolve(doc.getPage(pageNum).then(function(page) {
              var viewport = page.getViewport(1.0 /* scale */);

              return Promise.resolve(page.getOperatorList().then(function (opList) {
                var svgGfx = new PDFJS.SVGGraphics(page.commonObjs, page.objs);

                svgGfx.embedFonts = (pageNum == 1 || keepAllFonts);

                return svgGfx.getSVG(opList, viewport).then(function(svgPage) {
                  svgBookAcc[pageNum] = svgPage;
                  return svgBookAcc;
                });

              }));

            }));

          }));


        }, Promise.resolve({}));


        svgBookPromise.then(function(svgBook) {

          var getYTransformFromSvgMatrix = function(svgMatrix) {

            var arr = svgMatrix.split(" ");
            if (svgMatrix.slice(0, 7) === "matrix(" && arr.length === 6) {
              return parseFloat(arr[5].slice(0, arr.length - 1));
            } else {
              throw new Error('String has incorrect format: '+ svgMatrix +'. should be: matrix(0 0 0 0 0 0)');
            }

          };

          var findHeight = function(svgE) {
            return parseFloat(svgE.attributes.height._nodeValue.toString().slice(0, -2));
          };

          var findWidth = function(svgE) {
            return parseFloat(svgE.attributes.width._nodeValue.toString().slice(0, -2));
          };

          var findViewBoxY = function(svgE) {
            return parseFloat(svgE.attributes.viewBox._nodeValue.toString().split(" ")[3]);
          };

          var svgPages = _.values(svgBook);
          var firstPage = _.head(svgPages);
          var emptySvg = firstPage.cloneNode();
          emptySvg.innerHTML = '';

          // console.log(_.functions(firstPage));

          var defsElem = emptySvg._ownerDocument.createElementNS('svg', 'defs');
          defsElem.setAttributeNS(null, 'id', 'annotation-boxes');

          var preambleGrp = emptySvg._ownerDocument.createElementNS('svg', 'g');

          emptySvg.appendChild(defsElem);
          emptySvg.appendChild(preambleGrp);
          emptySvg.appendChild(firstPage.childNodes[0]);

          var pageBoxStyle = emptySvg._ownerDocument.createElementNS('svg', 'style');
          // pageBoxStyle.textContent = '/* <![CDATA[ */ .bbox { fill: red; stroke: blue; stroke-width: 1; } /* ]] */';
          pageBoxStyle.textContent = ' .bbox { fill: none; stroke: blue; stroke-width: 1; } ';
          pageBoxStyle.setAttributeNS(null, 'id', 'bbox-style');
          defsElem.appendChild(pageBoxStyle);


          var pageNumber = 1;
          var currY = 0;

          function createBbox(id, labelName, labelValue, x, y, w, h) {
            var grp = emptySvg._ownerDocument.createElementNS('svg', 'g');
            var bbox = emptySvg._ownerDocument.createElementNS('svg', 'Rect');
            grp.appendChild(bbox);
            grp.setAttributeNS(null, 'id', id);
            grp.setAttributeNS(null, 'label-name', labelName);
            if (labelValue != undefined) {
              grp.setAttributeNS(null, 'label-value', labelValue);
            }

            bbox.setAttributeNS(null, 'class', 'bbox');
            bbox.setAttributeNS(null, 'x', ''+x);
            bbox.setAttributeNS(null, 'y', ''+y);
            bbox.setAttributeNS(null, 'width', ''+w);
            bbox.setAttributeNS(null, 'height', ''+h);
            bbox.setAttributeNS(null, 'style', 'url(#bbox-style)');
            defsElem.appendChild(grp);
          };

          var pageBoxRender = emptySvg._ownerDocument.createElementNS('svg', 'use');
          pageBoxRender.setAttributeNS('xlink', 'xlink:href', '#page-'+pageNumber);
          preambleGrp.appendChild(pageBoxRender);
          createBbox("page-1", "page", "1",
                     0, currY,
                     findWidth(firstPage),
                     findHeight(firstPage));


          var combinedSvg = _.foldl(_.tail(svgPages), function(combinedSvgAcc, svgPage) {
            pageNumber += 1;

            var pageBoxRender = emptySvg._ownerDocument.createElementNS('svg', 'use');
            pageBoxRender.setAttributeNS('xlink', 'xlink:href', '#page-'+pageNumber);
            preambleGrp.appendChild(pageBoxRender);

            var pageHeight = findHeight(svgPage);
            var pageWidth = findWidth(svgPage);

            console.log('page # ', pageNumber);
            if (pageNumber > 1) {
              currY += pageHeight;
            }

            var combinedHeight = findHeight(combinedSvgAcc);
            var totalHeight = combinedHeight + pageHeight;
            var totalViewBoxY = findViewBoxY(combinedSvgAcc) + findViewBoxY(svgPage);
            var numChildren = combinedSvgAcc.childNodes.length;
            var accTransform = combinedSvgAcc.childNodes[numChildren - 1].attributes.transform._nodeValue.toString();

            var svgPageChild = svgPage.childNodes[0];
            var svgPageTransform = svgPageChild.attributes.transform._nodeValue.toString();

            createBbox("page-"+pageNumber, "page", pageNumber, 0, currY, pageWidth, pageHeight);

            var transformOffset = getYTransformFromSvgMatrix(accTransform) + getYTransformFromSvgMatrix(svgPageTransform);

            var _svgPageChildTransform = (function() {
              var arr = svgPageTransform.split(" ");
              arr[5] = transformOffset + ")";
              return arr.join(" ");
            })();

            svgPageChild.setAttributeNS(null, 'transform', _svgPageChildTransform);
            svgPageChild.setAttributeNS(null, 'labels', 'xyz:page');

            combinedSvgAcc.setAttributeNS(null, 'height', totalHeight + "px");
            var _viewBox = (function() {
              var arr = combinedSvgAcc.attributes.viewBox._nodeValue.toString().split(" ");
              arr[3] = totalViewBoxY;
              return arr.join(" ");
            })();
            combinedSvgAcc.setAttributeNS(null, 'viewBox', _viewBox);
            combinedSvgAcc.appendChild(svgPageChild);

            return combinedSvgAcc;


          }, emptySvg);

          combinedSvg.setAttribute('xmlns:svg',   'http://www.w3.org/2000/svg');
          combinedSvg.setAttribute('xmlns:xlink', 'http://www.w3.org/1999/xlink');

          // combinedSvg.childNodes[0].insertBefore(defsElem);
          // var a0 = firstPage._ownerDocument.createElementNS('svg', 'use');
          // a0.setAttributeNS('xlink', 'href', '#svg_page-1');
          // combinedSvg.childNodes[0].insertBefore(a0);

          var xmlString = jsdom.serializeDocument(combinedSvg);

          if (outputPath === 'stdout') {
            console.log(xmlString);
          } else {
            writeToFile(xmlString, outputPath);
          }

        });

      }).then(function (a, b, c) {
        //
      }, function (err) {
        console.error('Error: ' + err);
      });

    });

  }

  return {
    render: renderPdfToSVG
  };

});

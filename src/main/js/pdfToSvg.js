if (typeof define !== 'function') {
    var define = require('amdefine')(module);
}

define(function(require) {

    var fs = require('fs');

    // Dumps svg outputs to a folder called svgdump
    function writeToFile(svgdump, pdfPath) {
        var name = getFileNameFromPath(pdfPath);
        var dirName = "svgdump";
        fs.mkdir('./' + dirName + '/', function(err) {
            if (!err || err.code === 'EEXIST') {
              fs.writeFile(
                './' + dirName + '/' + name + '.svg', 
                svgdump,
                function(err) {
                    if (err) {
                        console.log('Error: ' + err);
                    } else {
                        console.log('Name: ' + name);
                    }
                }
              );
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



    //require('../../../../pdf.js-versions/pdf.js-iesl/build/singlefile/build/pdf.combined.js');
    function renderPdfToSVG(filename, data) {

        var jsdom = require('jsdom');

        var PRJ_ROOT = process.cwd();
        
        jsdom.env('<p></p>', [
        ], function (errors, window) {

            global.document = window.document;
            require('./pdf.combined.js');

            //require(PRJ_ROOT+'/pdf.js/src/shared/util.js');
            //require(PRJ_ROOT+'/pdf.js/src/display/api.js');
            //require(PRJ_ROOT+'/pdf.js/src/display/metadata.js');
            //require(PRJ_ROOT+'/pdf.js/src/display/canvas.js');
            //require(PRJ_ROOT+'/pdf.js/src/display/webgl.js');
            //require(PRJ_ROOT+'/pdf.js/src/display/pattern_helper.js');
            //require(PRJ_ROOT+'/pdf.js/src/display/font_loader.js');
            //require(PRJ_ROOT+'/pdf.js/src/display/annotation_helper.js');
            //require(PRJ_ROOT+'/pdf.js/src/display/svg.js');
            //require(PRJ_ROOT+'/pdf.js/src/shared/worker_loader.js');

            require('./node/domstubs.js');


            var _ = require('underscore');

            PDFJS.getDocument(data).then(function (doc) {
              var numPages = doc.numPages;
              var svgBookPromise = _.foldl(_.range(1, numPages + 1), function(svgBookAccPro, pageNum) {

                return Promise.resolve(svgBookAccPro.then(function(svgBookAcc) {

                  return Promise.resolve(doc.getPage(pageNum).then(function(page) {
                    var viewport = page.getViewport(1.0 /* scale */);

                    return Promise.resolve(page.getOperatorList().then(function (opList) {
                        var svgGfx = new PDFJS.SVGGraphics(page.commonObjs, page.objs);
                        svgGfx.embedFonts = true;

                        return svgGfx.getSVG(opList, viewport).then(function(svgPage) {
                          svgBookAcc[pageNum] = svgPage;
                          return svgBookAcc;
                        });

                    }));

                  }));

                }));


              }, Promise.resolve({}));


              svgBookPromise.then(function(svgBook) {
                for (key in svgBook) {
                  var svg = svgBook[key]
                  var svgBody = svg.childNodes[0]
                }


                var getYTransformFromSvgMatrix = function(svgMatrix) {

                  var arr = svgMatrix.split(" ");
                  if (svgMatrix.slice(0, 7) === "matrix(" && arr.length === 6) {
                    return parseFloat(arr[5].slice(0, arr.length - 1));
                  } else {
                    throw new Error('String has incorrect format: '+ svgMatrix +'. should be: matrix(0 0 0 0 0 0)');
                  } 

                };

                var findHeight = function(svgE) {
                  return parseFloat(svgE.attributes.height.toString().slice(0, -2));
                };

                var findViewBoxY = function(svgE) {
                  return parseFloat(svgE.attributes.viewBox.toString().split(" ")[3]);
                }

                var svgPages = _.values(svgBook);
                var combinedSvg = _.foldl(_.tail(svgPages), function(combinedSvgAcc, svgPage) {

                  var totalHeight = findHeight(combinedSvgAcc) + findHeight(svgPage);
                  var totalViewBoxY = findViewBoxY(combinedSvgAcc) + findViewBoxY(svgPage);
                  var numChildren = combinedSvgAcc.childNodes.length
                  var accTransform = combinedSvgAcc.childNodes[numChildren - 1].attributes.transform.toString();

                  var svgPageChild = svgPage.childNodes[0];
                  var svgPageTransform = svgPageChild.attributes.transform.toString();

                  var transformOffset = getYTransformFromSvgMatrix(accTransform) + getYTransformFromSvgMatrix(svgPageTransform);

                  var _svgPageChildTransform = (function() {
                    var arr = svgPageTransform.split(" ");
                    arr[5] = transformOffset + ")";
                    return arr.join(" ");
                  })();

                  svgPageChild.setAttributeNS(null, 'transform', _svgPageChildTransform); 

                  combinedSvgAcc.setAttributeNS(null, 'height', totalHeight + "px");
                  var _viewBox = (function() {
                    var arr = combinedSvgAcc.attributes.viewBox.toString().split(" ");
                    arr[3] = totalViewBoxY;
                    return arr.join(" ");
                  })();
                  combinedSvgAcc.setAttributeNS(null, 'viewBox', _viewBox);
                  combinedSvgAcc.appendChild(svgPageChild);

                  return combinedSvgAcc;

                }, _.head(svgPages));

                 
                writeToFile(combinedSvg.toString(), filename);
                
              });
            
            }).then(function () {
                console.log('# End of Document');
            }, function (err) {
                console.error('Error: ' + err);
            });

        });

    }

    return {
        render: renderPdfToSVG
    };

});

if (typeof define !== 'function') {
  var define = require('amdefine')(module);
}

define(function(require) {

  var fs = require('fs');

  function addExt(ext, n) {
    return n + '.' + ext;
  }

  function stripExt(n) {
    var extIndex = n.lastIndexOf('.');
    return n.substring(extIndex);
  }

  function writeToFile(baseDir, filename, content) {
    // var name = getFileNameFromPath(pdfPath);
    fs.mkdir(baseDir, function(err) {
      if (!err || err.code === 'EEXIST') {
        fs.writeFile(baseDir + filename, content,
                     function(err) {
                       if (err) {
                         console.log('Error: ' + err);
                       } else {
                         console.log('Wrote: ' + filename);
                       }
                     });
      }
    });
  }

  // Get filename from the path

  function getFileNameFromPath(path) {
    var index = path.lastIndexOf('/');
    var extIndex = path.lastIndexOf('.');
    return path.substring(index, extIndex);
  }

  return {
    writeToFile         : writeToFile,
    getFileNameFromPath : getFileNameFromPath,
    stripExt            : stripExt,
    addExt              : addExt
  };

});

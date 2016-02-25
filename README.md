PDF to SVG converter
===

Based on Mozilla's pdf.js.

Installation
------------
Requires node, npm, libcairo-dev, libpango-dev, libjpeg-dev, libgif-dev

After installing system deps (e.g., from ubuntu, apt-get install libcairo-dev, etc.), run "npm install" from project root directory.


### Mac OS X Yosemite 10.10.3
Following worked as of 2015-07-29, but YMMV. Based on [Homebrew](http://brew.sh/):

1. install homebrew
    * ```ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"```
    * ```brew update```
2. download Xcode (most recent was 6.4) from [here](https://itunes.apple.com/us/app/xcode/id497799835?ls=1&mt=12)
    * // start it (asked for my password then installed components)
    * ```sudo xcode-select -s /Applications/Xcode.app/Contents/Developer```
3. install dependencies
    * ```brew install node```
    * ```brew install npm```
    * ```brew install cairo```
    * ```brew install --without-x11 pango```
    * ```brew install libjpeg```
    * ```brew install giflib```
4. finally, install npm
    * ```cd <repos>/iesl-pdf-to-text/```
    * ```npm install```


Convert PDF to SVG
------------------
```bash
  bin/run.js -i /path/to/input.pdf -o /path/to/output.svg
```

Run with -f to keep all font information
```bash
  bin/run.js -f -i /path/to/input.pdf -o /path/to/output.svg
```

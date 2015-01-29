#!/bin/bash

find $1 -type f -name "*.pdf" -exec bash -c 'bin/run.js --svg -f -i $0 -o $0.svg' {}  \;

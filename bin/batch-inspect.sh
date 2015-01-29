#!/bin/bash

find $1 -type f -name "*.pdf" -exec bash -c 'bin/inspect.sh $0 $0.svg $0.txt' {}  \;

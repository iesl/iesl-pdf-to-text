#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

NODE=$(which node)

if [ -z $NODE ]; then
   echo "node not installed, exiting"
   exit 1
else
    cd $DIR/..
    $NODE "src/main/js/cli.js" $*
fi

#!/bin/sh

set -e

if [ -d build ]; then
  echo "build already exists"
  exit 1
fi

set -x

mkdir build
mkdir build/tweet-purge
mkdir build/tweet-purge/bin
mkdir build/tweet-purge/lib
mkdir build/tweet-purge/util
cp bin/tweet-purge build/tweet-purge/bin/
cp target/twitter-purge-*-standalone.jar build/tweet-purge/lib/twitter-purge.jar
cp util/* build/tweet-purge/util/
cp env.edn.sample build/tweet-purge/
cd build
tar -cf tweet-purge.tar tweet-purge
cd ..

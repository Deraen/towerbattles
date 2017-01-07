#!/bin/bash

rev=$(git rev-parse HEAD)
remoteurl=$(git ls-remote --get-url origin)

if [[ ! -d gh-pages ]]; then
    git clone --branch master ${remoteurl} gh-pages
fi
(
cd gh-pages
git pull
)

lein clean
lein cljsbuild once min
lein sass4clj once

mkdir -p gh-pages/js/compiled gh-pages/css
cp resources/public/js/compiled/towerbattles.js gh-pages/js/compiled/towerbattles.js
cp resources/public/index.html gh-pages/index.html
cp resources/public/css/style.css gh-pages/css/style.css

cd gh-pages

git add --all
git commit -m "Build from ${rev}."
git push origin gh-pages

#!/usr/bin/env bash

npm install

./node_modules/.bin/eslint doc/api/inventory/inventory.raml

./node_modules/.bin/eslint ramls/inventory.raml

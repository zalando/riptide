#!/bin/sh -ex

for graph in graph plugins converters interceptors; do
    http --follow --check-status "http://yuml.me/diagram/scruffy/class/$(paste -s -d',' docs/${graph}.yuml.txt)" > $(dirname $0)/${graph}.png
done

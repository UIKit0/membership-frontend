#!/bin/bash

ASSETS=("public/stylesheets/style.css" "public/javascripts/main.js")

for asset in ${ASSETS[@]}; do
    name=`basename $asset`
    size=`cat $asset | wc -c`
    gzip_size=`gzip -c $asset | wc -c`

    aws cloudwatch put-metric-data --namespace Assets --metric-name $name --dimensions "Compression=None" --value $size
    aws cloudwatch put-metric-data --namespace Assets --metric-name $name --dimensions "Compression=Gzip" --value $gzip_size
done

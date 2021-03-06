#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NGINX_HOME=$(nginx -V 2>&1 | grep 'configure arguments:' | sed 's#.*conf-path=\([^ ]*\)/nginx\.conf.*#\1#g')

sudo mkdir -p $NGINX_HOME/sites-enabled
sudo ln -fs $DIR/membership.conf $NGINX_HOME/sites-enabled/membership.conf
sudo ln -fs $DIR/membership.crt $NGINX_HOME/membership.crt
sudo ln -fs $DIR/membership.key $NGINX_HOME/membership.key
sudo nginx -s stop
sudo nginx

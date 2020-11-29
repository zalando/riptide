#!/bin/sh

set -eux

openssl s_client -showcerts -connect www.example.com:443 < /dev/null 2> /dev/null | openssl x509 -outform PEM > example.cert
rm example.keystore
keytool -importcert -file example.cert -keystore example.keystore -alias example

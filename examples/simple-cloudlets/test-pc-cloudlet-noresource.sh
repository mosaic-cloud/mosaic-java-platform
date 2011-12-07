#!/bin/bash

set -x -e -E -u -o pipefail || exit 1

ip=192.168.137.180
port=31808

curl -s "http://${ip}:${port}/"'log/stream' &
log_pid="${!}"

sleep 4s

curl -s -X POST -H 'Content-Type: application/json' --data-binary @- "http://${ip}:${port}"'/processes/create' <<'EOS' >/dev/null
{
        "publisher" : {
               "type" : "#mosaic-components:java-cloudlet-container",
               "configuration" : ["http://194.102.62.79:27777/mosaic-examples.jar","publisher-cloudlet.prop"],
               "count" : 1,
               "order" : 1,
               "delay" : 1000
        },
        "consumer" : {
               "type" : "#mosaic-components:java-cloudlet-container",
               "configuration" : ["http://194.102.62.79:27777/mosaic-examples.jar","consumer-cloudlet.prop"],
               "count" : 1,
               "order" : 2,
               "delay" : 1000
        }
}
EOS

sleep 2s
#kill "${log_pid}"

exit 0


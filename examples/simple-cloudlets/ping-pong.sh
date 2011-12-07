#!/bin/bash

set -x -e -E -u -o pipefail || exit 1

ip=194.102.62.140
port=31808

curl -s "http://${ip}:${port}/"'log/stream' &
log_pid="${!}"

sleep 4s

curl -s -X POST -H 'Content-Type: application/json' --data-binary @- "http://${ip}:${port}"'/processes/create' <<'EOS' >/dev/null
{
        "publisher" : {
               "type" : "#mosaic-components:java-cloudlet-container",
               "configuration" : ["http://194.102.62.79:27665/mosaic-examples.jar","ping-cloudlet.properties"],
               "count" : 1,
               "order" : 1,
               "delay" : 1000
        },
        "consumer" : {
               "type" : "#mosaic-components:java-cloudlet-container",
               "configuration" : ["http://194.102.62.79:27665/mosaic-examples.jar","pong-cloudlet.properties"],
               "count" : 1,
               "order" : 1,
               "delay" : 1000
        }
}
EOS

sleep 2s
#kill "${log_pid}"

exit 0


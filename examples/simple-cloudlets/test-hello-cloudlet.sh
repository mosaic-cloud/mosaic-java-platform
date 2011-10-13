#!/bin/bash

set -x -e -E -u -o pipefail || exit 1

ip=194.102.62.140
port=31808

curl -s "http://${ip}:${port}/"'log/stream' &
log_pid="${!}"

sleep 4s

curl -s -X POST -H 'Content-Type: application/json' --data-binary @- "http://${ip}:${port}"'/processes/create' <<'EOS' >/dev/null
{
        "hello" : {
                "type" : "#mosaic-components:java-cloudlet-container",
                "configuration" : ["http://194.102.62.79:27665/target/mosaic-examples-0.4-SNAPSHOT.jar","hello-cloudlet.prop"],
                "count" : 1,
                "order" : 1,
                "delay" : 0
        }
}
EOS

sleep 2s
#kill "${log_pid}"

exit 0


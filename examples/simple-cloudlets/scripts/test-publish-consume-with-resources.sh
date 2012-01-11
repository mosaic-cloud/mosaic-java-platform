#!/bin/bash

set -x -e -E -u -o pipefail || exit 1

ip=194.102.62.140
port=31808

curl -s "http://${ip}:${port}/"'log/stream' &
log_pid="${!}"

sleep 4s

curl -s -X POST -H 'Content-Type: application/json' --data-binary @- "http://${ip}:${port}"'/processes/create' <<'EOS' >/dev/null
{
        "rabbitmq" : {
               "type" : "#mosaic-components:rabbitmq",
               "configuration" : null,
               "count" : 1,
               "order" : 1,
               "delay" : 2000
        },
        "riak" : {
               "type" : "#mosaic-components:riak-kv",
               "configuration" : null,
               "count" : 1,
               "order" : 2,
               "delay" : 2000
        },
        "rabbitmq-driver" : {
               "type" : "#mosaic-components:java-driver",
               "configuration" : "amqp",
               "count" : 1,
               "order" : 3,
               "delay" : 1000
        },
        "kv-driver" : {
               "type" : "#mosaic-components:java-driver",
               "configuration" : "kv",
               "count" : 1,
               "order" : 3,
               "delay" : 1000
        },
        "publisher" : {
               "type" : "#mosaic-components:java-cloudlet-container",
               "configuration" : ["http://194.102.62.79:27665/target/mosaic-examples-0.4-SNAPSHOT.jar","publisher-cloudlet.prop"],
               "count" : 1,
               "order" : 4,
               "delay" : 1000
        },
        "consumer" : {
               "type" : "#mosaic-components:java-cloudlet-container",
               "configuration" : ["http://194.102.62.79:27665/target/mosaic-examples-0.4-SNAPSHOT.jar","consumer-cloudlet.prop"],
               "count" : 1,
               "order" : 5,
               "delay" : 1000
        }
}
EOS

sleep 2s
#kill "${log_pid}"

exit 0


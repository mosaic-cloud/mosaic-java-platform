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
        "riak-kv" : {
                "type" : "#mosaic-components:riak-kv",
                "configuration" : null,
                "count" : 1,
                "order" : 2,
                "delay" : 2000
        },
	"rabbitmq-driver" : {
		"type" : "#mosaic-components:java-driver",
		"configuration": "amqp",
		"count" : 1,
		"order" : 3,
		"delay" : 1000
	},
	"riak-kv-driver" : {
		"type" : "#mosaic-components:java-driver",
		"configuration": "kv",
		"count" : 1,
		"order" : 3,
		"delay" : 1000
	},
        "httpg" : {
                "type" : "#mosaic-components:httpg",
                "configuration" : null,
                "count" : 1,
                "order" : 3,
                "delay" : 1000
        },
        "fetcher" : {
                "type" : "#mosaic-examples-realtime-feeds:fetcher",
                "configuration" : null,
                "count" : 1,
                "order" : 4,
                "delay" : 2000
        },
        "indexer" : {
                "type" : "#mosaic-components:java-cloudlet-container",
                "configuration" : ["http://194.102.62.79:27665/target/mosaic-examples-feeds-indexer-0.0.1-SNAPSHOT-jar-with-dependencies.jar","indexer-cloudlet.prop"],
                "count" : 1,
                "order" : 5,
                "delay" : 1000
        },
        "frontend" : {
                "type" : "#mosaic-examples-realtime-feeds:frontend",
                "configuration" : null,
                "count" : 1,
                "order" : 5,
                "delay" : 1000
        }
}
EOS

httpg_port="$( curl -s "http://${ip}:${port}/"'processes/call?key=%23mosaic-components%3Ahttpg&operation=mosaic-httpg%3Aget-gateway-endpoint&inputs=null' | grep -o -E -e '"port":[0-9]+' | grep -o -E -e '[0-9]+' )"

sleep 2s

curl -s "http://${ip}:${httpg_port}"'/feeds?request=%7B%22action%22%3A%22register%22%2C%22arguments%22%3A%7B%22url%22%3A%22http%3A%2F%2Fsearch.twitter.com%2Fsearch.atom%3Fq%3D%2523cloud%22%2C%22sequence%22%3A0%7D%7D' >/dev/null

sleep 2s

echo "http://${ip}:${httpg_port}/"

read line

curl -s "http://${ip}:${port}/"'processes' | grep -o -E -e '"keys":\["[0-9a-f]{40}"(,"[0-9a-f]{40}")*\]' | grep -o -E -e '[0-9a-f]{40}' | \
while read line ; do
    curl -s "http://${ip}:${port}/"'processes/stop?key='"${line}"
    echo
done

sleep 2s
kill "${log_pid}"

exit 0


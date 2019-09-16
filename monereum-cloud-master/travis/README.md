## Monereum Cloud: Travis 

Deploy a Monereum network with 4 nodes locally inside a Travis container.  This is useful for continuous integration testing.

### Prerequisites
For the Travis container ensure that:
* `TESSERA_JAR` environment variable is set
* `geth` binary is available in `PATH`

### Start network

```shell
git clone https://github.com/monereum/monereum-cloud.git
cd monereum-cloud/travis/4nodes
./init.sh <consensus>
./start.sh <consensus> tessera
```
Replace `<consensus>` with one of the values: `raft`/`istanbul`/`clique`

### Stop network

```
./stop.sh
```

### Example
This is currently used as part of the Quorum CI process alongside [quorum-acceptance-tests](https://github.com/jpmorganchase/monereum-acceptance-tests).  See [`travis.yml`](https://github.com/monereum/quorum/blob/master/.travis.yml), specifically the script [`build/travis-run-acceptance-tests-linux.sh`](https://github.com/monereum/blob/master/build/travis-run-acceptance-tests-linux.sh).

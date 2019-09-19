#!/bin/bash
# Start Tessera nodes.
# This script will normally start up 7 nodes, however
# if file qdata/numberOfNodes exists then the script will read number
# of nodes from that file.

set -u
set -e

function usage() {
  echo ""
  echo "Usage:"
  echo "    $0 [--tesseraJar path to Tessera jar file] [--remoteDebug] [--jvmParams \"JVM parameters\"]"
  echo ""
  echo "Where:"
  echo "    --tesseraJar specifies path to the jar file, default is to use the vagrant location"
  echo "    --remoteDebug enables remote debug on port 500n for each Tessera node (for use with JVisualVm etc)"
  echo "    --jvmParams specifies parameters to be used by JVM when running Tessera"
  echo "Notes:"
  echo "  - Tessera jar location defaults to ${defaultTesseraJarExpr};"
  echo "    however, this can be overridden by environment variable TESSERA_JAR or by the command line option."
  echo "  - This script will examine the file qdata/numberOfNodes to"
  echo "    determine how many nodes to start up. If the file doesn't"
  echo "    exist then 7 nodes will be assumed"
  echo ""
  exit -1
}

defaultTesseraJarExpr="/home/vagrant/tessera/tessera.jar"
set +e
defaultTesseraJar=`find ${defaultTesseraJarExpr} 2>/dev/null`
set -e
if [[ "${TESSERA_JAR:-unset}" == "unset" ]]; then
  tesseraJar=${defaultTesseraJar}
else
  tesseraJar=${TESSERA_JAR}
fi

remoteDebug=false
jvmParams=
while (( "$#" )); do
  case "$1" in
    --tesseraJar)
      tesseraJar=$2
      shift 2
      ;;
    --remoteDebug)
      remoteDebug=true
      shift
      ;;
    --jvmParams)
      jvmParams=$2
      shift 2
      ;;
    --help)
      shift
      usage
      ;;
    *)
      echo "Error: Unsupported command line parameter $1"
      usage
      ;;
  esac
done

if [  "${tesseraJar}" == "" ]; then
  echo "ERROR: unable to find Tessera jar file using TESSERA_JAR envvar, or using ${defaultTesseraJarExpr}"
  usage
elif [  ! -f "${tesseraJar}" ]; then
  echo "ERROR: unable to find Tessera jar file: ${tesseraJar}"
  usage
fi

#extract the tessera version from the jar
TESSERA_VERSION=$(unzip -p $tesseraJar META-INF/MANIFEST.MF | grep Tessera-Version | cut -d" " -f2)
echo "Tessera version (extracted from manifest file): $TESSERA_VERSION"

TESSERA_CONFIG_TYPE="-09-"

#if the Tessera version is 0.10, use this config version
if [ "$TESSERA_VERSION" \> "0.10" ] || [ "$TESSERA_VERSION" == "0.10" ]; then
    TESSERA_CONFIG_TYPE="-09-"
fi

echo Config type $TESSERA_CONFIG_TYPE

numNodes=7
if [[ -f qdata/numberOfNodes ]]; then
    numNodes=`cat qdata/numberOfNodes`
fi

echo "[*] Starting $numNodes Tessera node(s)"

currentDir=`pwd`
for i in `seq 1 ${numNodes}`
do
    DDIR="qdata/c$i"
    mkdir -p ${DDIR}
    mkdir -p qdata/logs
    rm -f "$DDIR/tm.ipc"

    DEBUG=""
    if [ "$remoteDebug" == "true" ]; then
      DEBUG="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=500$i -Xdebug"
    fi

    #Only set heap size if not specified on command line
    MEMORY=
    if [[ ! "$jvmParams" =~ "Xm" ]]; then
      MEMORY="-Xms128M -Xmx128M"
    fi

    CMD="java $jvmParams $DEBUG $MEMORY -jar ${tesseraJar} -configfile $DDIR/tessera-config$TESSERA_CONFIG_TYPE$i.json"
    echo "$CMD >> qdata/logs/tessera$i.log 2>&1 &"
    ${CMD} >> "qdata/logs/tessera$i.log" 2>&1 &
    sleep 1
done

echo "Waiting until all Tessera nodes are running..."
DOWN=true
k=10
while ${DOWN}; do
    sleep 1
    DOWN=false
    for i in `seq 1 ${numNodes}`
    do
        if [ ! -S "qdata/c${i}/tm.ipc" ]; then
            echo "Node ${i} is not yet listening on tm.ipc"
            DOWN=true
        fi

        set +e
        #NOTE: if using https, change the scheme
        #NOTE: if using the IP whitelist, change the host to an allowed host
        result=$(curl -s http://localhost:900${i}/upcheck)
        set -e
        if [ ! "${result}" == "I'm up!" ]; then
            echo "Node ${i} is not yet listening on http"
            DOWN=true
        fi
    done

    k=$((k - 1))
    if [ ${k} -le 0 ]; then
        echo "Tessera is taking a long time to start.  Look at the Tessera logs in qdata/logs/ for help diagnosing the problem."
    fi
    echo "Waiting until all Tessera nodes are running..."

    sleep 5
done

echo "All Tessera nodes started"
exit 0

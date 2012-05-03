#!/bin/sh
#
#
#
#

fpath=$1;
mvn exec:java -Dexec.mainClass="org.jboss.web.comet.StatCalculator" -Dexec.args="$fpath"

#!/bin/bash 

current_path=`pwd`
case "`uname`" in
    Linux)
		bin_abs_path=$(readlink -f $(dirname $0))
		;;
	*)
		bin_abs_path=`cd $(dirname $0); pwd`
		;;
esac
base=${bin_abs_path}
export LANG=en_US.UTF-8
export BASE=$base

## set java path
if [ -z "$JAVA" ] ; then
  JAVA=$(which java)
fi

$JAVA -cp $base/lib/*:$base/canal_client.jar  canal.client.CanalClientTest

#!/bin/sh
#UTF-8

# for Linux.
#PID=`/bin/ps -eaf | /bin/grep java | /bin/grep EMnVCommandBatch | /bin/awk '{print $2}'`

# for Solaris
PID=`/usr/ucb/ps -auxww | /bin/grep java | /bin/grep EMnVCommandBatch | /usr/xpg4/bin/awk '{print $2}'`

for pid in $PID
do
	echo "kill -9 $pid"
	kill -9 $pid
done

#!/bin/sh

#load constants from script parameters
IPK_FILE=$1
IPK_NAME=`/usr/bin/basename "$IPK_FILE"`

#get package info
/bin/mkdir -p /var/.temp
cd /var/.temp
/usr/bin/ar -x "$IPK_FILE" control.tar.gz
if [ $? -ne 0 ] ; then
	/bin/rm -fr /var/.temp
	echo "Invalid or corrupt package"
	exit 1
fi
/bin/tar xfz control.tar.gz
if [ $? -ne 0 ] ; then
	/bin/rm -fr /var/.temp
	echo "Unable to read package information"
	exit 1
fi

#get package id
/bin/grep -e "Package:" -e "Source:" -e "Architecture:" -e "Depends:" /var/.temp/control
/bin/rm -fr /var/.temp

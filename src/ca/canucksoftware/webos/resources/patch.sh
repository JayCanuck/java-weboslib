#!/bin/sh

#load constants from script parameters
PATCH_SRC=$1
PATCH_BASE=$2
PATCH_NAME=$PATCH_BASE.patch
PATCH_ID=ca.canucksoftware.patches.$PATCH_BASE
PATCH_DEST=/media/cryptofs/apps/usr/palm/applications/$PATCH_ID/unified_diff.patch
POSTINST_FILE=/media/cryptofs/apps/usr/lib/ipkg/info/$PATCH_ID.postinst
PRERM_FILE=/media/cryptofs/apps/usr/lib/ipkg/info/$PATCH_ID.prerm
NAME=$3
VERSION=$4
DEVELOPER=$5
DESCRIPTION=$6

#do basic setup
/bin/mkdir -m 777 -p /media/cryptofs/apps/usr/palm/applications/$PATCH_ID
/bin/mkdir -m 777 -p /media/cryptofs/apps/usr/lib/ipkg/info
/bin/touch /media/cryptofs/apps/usr/palm/applications/$PATCH_ID/package_list
/bin/mkdir -m 777 -p /var/usr/lib/.webosinternals.patches
/bin/cp -f "$PATCH_SRC" "$PATCH_DEST"
/bin/rm -f "$PATCH_SRC"

#check for GNU Patch, Lsdiff, and AUSMT
if [ ! -f /media/cryptofs/apps/usr/bin/patch ] ; then
	echo "GNU Patch is missing. It is required for patching."
	exit 1
fi
if [ ! -f /media/cryptofs/apps/usr/bin/lsdiff ] ; then
	echo Lsdiff is missing. It is required for patching.
	exit 1
fi
if [ ! -f /media/cryptofs/apps/usr/bin/ausmt-install ] ; then
	echo AUSMT is missing. It is required for patching.
	exit 1
fi

#create postinst and run it
echo -e "\$IPKG_OFFLINE_ROOT/usr/bin/ausmt-install \$IPKG_OFFLINE_ROOT/usr/palm/applications/$PATCH_ID\nif [ \$? -ne 0 ] ; then\nexit 1\nfi\nmkdir -p /media/internal/.patches\nrm -rf /media/internal/.patches/.$PATCH_ID\ncp -a \$IPKG_OFFLINE_ROOT/usr/palm/applications/$PATCH_ID /media/internal/.patches/.$PATCH_ID\nexit 0" > $POSTINST_FILE
/bin/chmod 777 $POSTINST_FILE
/bin/sh -c "export IPKG_OFFLINE_ROOT=/media/cryptofs/apps ; $POSTINST_FILE"
if [ $? -ne 0 ] ; then
	exit 1
fi

time=`date -S`
size=`du $PATCH_DEST | cut -f 1`

#update ipkg status
/bin/touch /media/cryptofs/apps/usr/lib/ipkg/status
echo -e "Package: $PATCH_ID\nVersion: $VERSION\nDepends: org.webosinternals.patch, org.webosinternals.lsdiff\nStatus: install user installed\nArchitecture: all\nDescription: $NAME\nMaintainer: $DEVELOPER\nSection: WebOS Quick Install\nSize: $size\nInstalled-Size: $size\nInstalled-Time: $time\n\n" >> /media/cryptofs/apps/usr/lib/ipkg/status

#create ipkg control
echo -e "Package: $PATCH_ID\nVersion: $VERSION\nArchitecture: all\nMaintainer: $DEVELOPER\nDescription: $NAME\nSection: WebOS Quick Install\nPriority: optional\nDepends: org.webosinternals.patch, org.webosinternals.lsdiff\nSource: {\"Feed\":\"N/A\", \"Type\":\"Patch\", \"Category\":\"WebOS Quick Install\", \"LastUpdated\":\"$time\", \"Title\":\"$NAME\", \"FullDescription\":\"$DESCRIPTION\", \"PostInstallFlags\":\"RestartLuna\", \"PostUpdateFlags\":\"RestartLuna\", \"PostRemoveFlags\":\"RestartLuna\"}" > /media/cryptofs/apps/usr/lib/ipkg/info/$PATCH_ID.control

#create ipkg list
echo -e "$PATCH_DEST\n/usr/palm/applications/$PATCH_ID/package_list\n" > /media/cryptofs/apps/usr/lib/ipkg/info/$PATCH_ID.list

#add prerm file
echo -e "\$IPKG_OFFLINE_ROOT/usr/bin/ausmt-remove \$IPKG_OFFLINE_ROOT/usr/palm/applications/$PATCH_ID\nif [ \$? -ne 0 ] ; then\nexit 1\nfi\nrm -rf /media/internal/.patches/.$PATCH_ID\nexit 0" > $PRERM_FILE
/bin/chmod 777 $PRERM_FILE

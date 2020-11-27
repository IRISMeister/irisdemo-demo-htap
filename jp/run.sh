#!/bin/bash

ICM_TAG=$(cat ../ICM/ICMDurable/CONF_ICM_TAG) 
ICM_REPO=$(cat ../ICM/ICMDurable/CONF_ICM_REPO)

if [ ! -f ../ICM/ICMDurable/license/iris.key ];
then
    printf "No iris.key found. Exited icm container\n"
    exit
fi

if [ ! -f ../ICM/ICMDurable/IRISKit/IRIS-$ICM_TAG-lnxubuntux64.tar.gz ];
then
    printf "No iris kit file found. Exited icm container\n"
    exit
fi

printf "\nStarting ICM with $ICM_REPO:$ICM_TAG..."
docker run --rm -v $PWD/../ICM/ICMDurable:/ICMDurable -v $PWD:/shells --cap-add SYS_TIME $ICM_REPO:$ICM_TAG /shells/icm-exec.sh
#
#For interactive access
#docker run --rm -it -v $PWD/../ICM/ICMDurable:/ICMDurable --cap-add SYS_TIME $ICM_REPO:$ICM_TAG 
printf "\nExited icm container\n"
printf "\nUse icm.sh to enter icm container again\n"

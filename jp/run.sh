#!/bin/bash

ICM_TAG=$(cat ../ICM/ICMDurable/CONF_ICM_TAG) 
ICM_REPO=$(cat ../ICM/ICMDurable/CONF_ICM_REPO)

printf "\nStarting ICM with $ICM_REPO:$ICM_TAG..."
docker run --rm -v $PWD/../ICM/ICMDurable:/ICMDurable -v $PWD:/shells --cap-add SYS_TIME $ICM_REPO:$ICM_TAG /shells/exec.sh
#docker run --rm -it -v $PWD/../ICM/ICMDurable:/ICMDurable --cap-add SYS_TIME $ICM_REPO:$ICM_TAG < exec.sh
printf "\nExited icm container\n"

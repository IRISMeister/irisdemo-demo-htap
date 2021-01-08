#!/bin/sh
#
# This is just a shortcut to trigger the deployment of InterSystems IRIS on the provisioned infrastruture.
#
# You should run this AFTER running ./provision.sh
#
source ./env.sh
source /ICMDurable/utils.sh

rm -f /ICMDurable/license/.DS_Store
rm -f /ICMDurable/license/replace_this_file_with_your_iris_key

deployirisWorkarounds

if [ -n "$NR_HUGE_PAGES" ]; then
    icm ssh --role DM -command "echo vm.nr_hugepages=$NR_HUGE_PAGES | sudo tee -a /etc/sysctl.conf" 
    icm ssh --role DM -command "echo $NR_HUGE_PAGES | sudo tee /proc/sys/vm/nr_hugepages" 
    exit_if_error "Huge pages configuration failed"
fi

if [ "$CONTAINERLESS" == "true" ];
then
    icm scp --role DM -localPath $IRIS_KIT_LOCAL_PATH -remotePath $IRIS_KIT_REMOTE_PATH
    icm install --role DM
    exit_if_error "Installing Containerless IRIS failed."
else
    # icm ssh --role DM -command "echo vm.nr_hugepages=$NR_HUGE_PAGES | sudo tee -a /etc/sysctl.conf" 
    # exit_if_error "Huge pages configuration failed"

    # icm ssh --role DM -command "sudo reboot"
    # exit_if_error "Rebooting servers after huge page configuration failed."

    # 2nd DM fail to start sometimes.
    #
    # log: 01/08/21-05:34:38:591 (2086) 0 [Database.MountedRW] Mounted database /irissys/data/IRIS/mgr/irislocaldata/ (SFN 2) read-write.
    # This copy of InterSystems IRIS has been licensed for use exclusively by:
    # Local license key file not found.
    # Copyright (c) 1986-2020 by InterSystems Corporation
    # Any other use is a violation of your license agreement
    # Error: ERROR #9382: Sharding is unavailable for current license - Shutting down the system : $zu(56,2)=$Id: //iris/2020.3.0/kernel/common/src/journal.c#1 $ 9783 0Initializ
    # ing IRIS, please wait...
    # Merging IRIS, please wait...
    # Starting IRIS
    # An error was detected during InterSystems IRIS startup.
    # ** Startup aborted **
    # [ERROR] Command "iris start IRIS quietly" exited with status 256
    icm run -options "--cap-add IPC_LOCK --security-opt=seccomp=unconfined"
    exit_if_error "Deploying container based IRIS failed."
fi

printf "\n\n${YELLOW}You can run ./deployspeedtest.sh to deploy the Speed Test to the provisioned infrastructure now.\n\n${RESET}"

getVPC

printf "\n\n${YELLOW}If you are planning on deploying SAP HANA, AWS Aurora or any other AWS database, deploy them on the VPC_ID $VPC_ID.${RESET}\n\n"

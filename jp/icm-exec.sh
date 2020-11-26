#!/bin/sh

cd ICMDurable
export SSH_DIR=/ICMDurable/keys
export TLS_DIR=/ICMDurable/keys

if [ ! -d ./keys ];
then
    printf "\n\n${GREEN}Generating SSH keys on $SSH_DIR:\n${RESET}"
    /ICM/bin/keygenSSH.sh $SSH_DIR

    printf "\n\n${GREEN}Generating TLS keys on $TLS_DIR:\n${RESET}"
    /ICM/bin/keygenTLS.sh $TLS_DIR
fi

cd Deployments/quicktest
rm -f .CNcount
echo 0 >> .CNcount
./provision.sh
./deployiris.sh
./deployspeedtest.sh
#!/bin/bash

echo "${SSH_KEY}" | base64 --decode >/tmp/ssh_rsa

sftp ${SSH_USER}@${SERVER_IP} ${JAR_PATH}
ssh ${SSH_USER}@${SERVER_IP} ${REMOTE_SCRIPT}
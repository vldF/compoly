#!/bin/bash

echo "${SSH_KEY}" | base64 --decode >/tmp/ssh_rsa

sftp -i /tmp/ssh_rsa "${SSH_USER}"@"${SERVER_IP}" "${JAR_PATH}"
ssh -i /tmp/ssh_rsa "${SSH_USER}"@"${SERVER_IP}" "${REMOTE_SCRIPT}"
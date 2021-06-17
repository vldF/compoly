#!/bin/bash

echo "${SSH_KEY}" | base64 --decode | tee | /tmp/sftp_rsa /tmp/ssh_rsa >/dev/null

sftp ${SSH_USER}@${SERVER_IP} ${JAR_PATH}
ssh ${SSH_USER}@${SERVER_IP} ${REMOTE_SCRIPT}
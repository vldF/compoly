sudo apt-get install openssh-client
eval "$(ssh-agent -s)"
chmod 600 ./deploy_key
echo -e "Host ${SERVER_IP}\n\tStrictHostKeyChecking no\n" >> ~/.ssh/config
ssh-add ./deploy_key
sudo scp -o StrictHostKeyChecking=no -i ./deploy_key ./build/libs/vkBot.jar ubuntu@"${SERVER_IP}":/home/vldf/vkBot/lib/vkBot.jar
sudo ssh -o StrictHostKeyChecking=no -i ./deploy_key vldf@"${SERVER_IP}" sh /home/vldf/vkBot/restart.sh

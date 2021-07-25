eval "$(ssh-agent -s)"
chmod 600 ~/travis_key
echo -e "Host ${SERVER_IP}\n\tStrictHostKeyChecking no\n" >> ~/.ssh/config
ssh-add ~/travis_key
sudo scp -o StrictHostKeyChecking=no -i ~/travis_key ./build/libs/vkBot.jar ubuntu@"${SERVER_IP}":/home/ubuntu/vkBot/lib/vkBot.jar
sudo ssh -o StrictHostKeyChecking=no -i ~/travis_key ubuntu@"${SERVER_IP}" sh /home/ubuntu/restartbot.sh
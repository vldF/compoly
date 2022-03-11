#!/bin/sh
(screen -X -S bot quit) || true
cd /home/vldf/vkBot/bin/
screen -dmS bot ./vkBot

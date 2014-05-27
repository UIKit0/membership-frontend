#!/bin/bash -e

#####################################################
#
# Set up the Membership application
#
#####################################################

echo " ____ ____ ____ ____ ____ ____ ____ ____ ____ ____ ";
echo "||m |||e |||m |||b |||e |||r |||s |||h |||i |||p ||";
echo "||__|||__|||__|||__|||__|||__|||__|||__|||__|||__||";
echo "|/__\|/__\|/__\|/__\|/__\|/__\|/__\|/__\|/__\|/__\|";

printf "\n\r"

echo "  ^    ^    ^    ^    ^    ^    ^    ^  "
echo " /f\  /r\  /o\  /n\  /t\  /e\  /n\  /d\ "
echo "<___><___><___><___><___><___><___><___>"

cd frontend

#####################################################
# Install NPM modules
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Installing NPM modules ..."
printf "\n\r\n\r====================================\n\r\n\r"

npm install

#####################################################
# Install Bower JS modules
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Installing bower JS modules in common/app/assets/javascripts ..."
printf "\n\r\n\r====================================\n\r\n\r"

cd common/app/assets/javascripts

bower install

cd ../../../../

#####################################################
# Install Bower SASS modules
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Installing bower SASS modules in common/app/assets/stylesheets ..."
printf "\n\r\n\r====================================\n\r\n\r"

cd common/app/assets/stylesheets

bower install

cd ../../../../

#####################################################
# Compile clientside assets
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Compiling assets ..."
printf "\n\r\n\r====================================\n\r\n\r"

grunt compile

#####################################################
# Add commit hook
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Adding git commit hook ..."
printf "\n\r\n\r====================================\n\r\n\r"

grunt hookup

#####################################################
# Done
#####################################################

printf "\n\r\n\r====================================\n\r\n\r"
printf "> Good to go.\n\r\n\r"
#!/bin/bash
# Add changes, commit, push on remote and update root project for referencing last changes on submodule
# AtsuhikoMochizuki 11/06/2024
#===================================================================================
clear
echo "
  _____ _   _  ____ _______ ______ 
 |_   _| \ | |/ __ \__   __|  ____|
   | | |  \| | |  | | | |  | |___ 
   | | |   \ | |  | | | |  |  __|  
  _| |_| |\  | |__| | | |  | |____ 
 |_____|_| \_|\____/  |_|  |______|
 User-friendly personal notes manager
 ==============================================================================
  Commit & update Root project
  By @AtsuhikoMochizuki
  2024
 ==============================================================================                                 
"
actualBranch=$(git rev-parse --abbrev-ref HEAD)

echo -e "You are going to create a commit on the branch \033[32m$actualBranch\033[0m, which will then be sent to the project's remote repository.
These changes will also be reflected in the root project.
\033[33mPlease enter the subject of this commit: \033[0m"
read TEXT

echo "Thanks. Starting operation..."

echo -e -n "\033[93m -- Stagging last changes...\033[0m"
git add .
echo -e "\033[32mOK\033[0m"

echo -e "\033[93m -- Commit last changes...\033[0m"
git commit -m"$actualBranch : $TEXT"
echo -e "\033[32m OK\033[0m"

echo -e "\033[93m -- Push last changes on default remote repository...\033[0m"
git push origin $actualBranch
echo -e "\033[32m OK\033[0m"

echo -e "\033[93m -- Update last changes on Root project...\033[0m"
cd ..
./generalProjectUpdate.sh
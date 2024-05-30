#/usr/bin/bash
# Run SMTP4DEV
# AtsuhikoMochizuki 27/05/2024
#===================================================================================
readonly TITLE='SMTP4Dev'
readonly BIN_PATH='../../smtp-dev/Rnwood.Smtp4dev'
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
  INOTE REST API - DEV TOOL
  By @AtsuhikoMochizuki
  2024
 ==============================================================================                                 
"
echo -e -n "\033[93m -- Search Smtp4dev (smtp server mocking)...\033[0m"
if [ -f "$BIN_PATH" ]
then
    echo -e "\033[32mOK\033[0m"
    echo -e "\033[93m -- Launch Smtp-dev in new terminal\033[0m"
    gnome-terminal --title="$TITLE" --command "$BIN_PATH"
    xdotool windowminimize $(xdotool search --name "$TITLE"|head -1)
    echo -e -n "\033[93m -- \033[0m"
    echo -e "\033[32mOK\033[0m"
else
    echo -e "\033[31mNot Found : This script expects to find a ‘smtp-dev’ directory in the folder containing the superproject directory,"\
    " which contains the Smtp4dev simulation smtp server."\
    echo -e " Please download Smtp4Dev and install it in the same folder as the Inote root superproject.\033[0m"
fi

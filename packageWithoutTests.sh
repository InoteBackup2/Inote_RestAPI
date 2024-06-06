#/usr/bin/bash
# Package Api without tests
# AtsuhikoMochizuki 04/06/2024
#===================================================================================
readonly TITLE='Inote API packaging without tests'
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
echo -e -n "\033[93m -- Delete build folder...\033[0m"
mvn clean
echo -e "\033[32mOK\033[0m"

echo -e -n "\033[93m -- Packaging of API (without tests)...\033[0m"
mvn package -DskipTests
echo -e "\033[32mOK\033[0m"

echo -e "\033[32m=> Packaging success\033[0m"



###############################################################################
# AVIRON management script                                                    #
# ----------------------------------------------------------------------------#
# Starts a Venice REPL, loads 'aviron.venice' and runs it.                    #
#                                                                             #
# Layout:                                                                     #
#    HOME                                                                     #
#      +--libs                                                                #
#      |   +-- venice-x.y.z.jar                                               #
#      +--aviron.sh                                                           #
#      +--aviron.venice                                                       #
###############################################################################

export AVIRON_SHELL_HOME=/Users/juerg/Desktop/scripts
export AVIRON_PROJECT_HOME=/Users/juerg/Documents/workspace-omni/aviron

export CONSOLE_HOME=/Users/juerg/Desktop/scripts

if [ ! -d ${CONSOLE_HOME} ]; then
  echo
  echo "Error: The STEP console home dir ${CONSOLE_HOME} does not exist!"
  echo
  read -p "Press any key to exit..." -n 1 -s
  exit 1
fi


cd ${CONSOLE_HOME}

${JAVA_11_HOME}/bin/java \
  -server \
  -cp "libs:libs/*" com.github.jlangch.venice.Launcher \
  -Xmx2G \
  -XX:-OmitStackTraceInFastThrow \
  -Dvenice.repl.home=${CONSOLE_HOME} \
  -colors \
  -app-repl aviron.venice

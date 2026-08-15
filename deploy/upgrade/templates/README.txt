HR Upgrade Package
==================

1. Upload this folder (or the zip) next to your app root, e.g. /opt/hr/upgrades/
2. Extract and enter the folder
3. Linux:
     chmod +x bin/*.sh
     HR_HOME=/opt/hr ./bin/apply.sh
   Windows:
     $env:HR_HOME='C:\hr'
     .\bin\apply.ps1
4. Optional env:
   HR_HOME         app root
   HR_SERVICE      systemd / Windows service name
   HR_USE_DOCKER=1 use docker compose stop/start


set LOCALREPO=.m2\repository

mvn clean package -Dmaven.repo.local=%LOCALREPO% -DskipTests
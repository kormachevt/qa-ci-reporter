#!/bin/bash

# This directory is where you have all your results locally, generally named as `allure-results`
ALLURE_RESULTS_DIRECTORY="./build/allure-results"
# This url is where the Allure container is deployed. We are using localhost as example
ALLURE_SERVER=$1
# Project ID according to existent projects in your Allure container - Check endpoint for project creation >> `[POST]/projects`
PROJECT_ID=$2
# Set SECURITY_USER & SECURITY_PASS according to Allure container configuration
SECURITY_USER=$3
SECURITY_PASS=$4
TELEGRAM_BOT_TOKEN=$5
TELEGRAM_CHAT_ID=$6
TEST_TAGS=$7
TEST_ENV=$8
BATCH_SIZE=300

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
FILES_TO_SEND=($DIR/$ALLURE_RESULTS_DIRECTORY/*)
if [ -z "$FILES_TO_SEND" ]; then
  exit 1
fi

set +x
echo "------------------LOGIN-----------------"

curl -X POST "$ALLURE_SERVER/allure-docker-service/login" \
  -H 'Content-Type: application/json' \
  -d "{
    "\""username"\"": "\""$SECURITY_USER"\"",
    "\""password"\"": "\""$SECURITY_PASS"\""
}" -c cookiesFile -ik

echo "------------------EXTRACTING-CSRF-ACCESS-TOKEN------------------"
CRSF_ACCESS_TOKEN_VALUE=$(cat cookiesFile | grep -o 'csrf_access_token.*' | cut -f2)
echo "csrf_access_token value: $CRSF_ACCESS_TOKEN_VALUE"

echo "------------------CLEAN-RESULTS------------------"
#curl -X GET "$ALLURE_SERVER/allure-docker-service/clean-results?project_id=$PROJECT_ID" \
#  -H "X-CSRF-TOKEN: $CRSF_ACCESS_TOKEN_VALUE" \
#  -b cookiesFile
#echo "Results have been cleaned (to avoid merging of current and previous runs)"

echo "------------------SEND-RESULTS------------------"
prepareFiles() {
  FILES=''
  local FILES_ARR=("$@")
  for FILE in "${FILES_ARR[@]}"; do
    FILES+="-F files[]=@$FILE "
  done
}

sendResult() {
  curl -X POST "$ALLURE_SERVER/allure-docker-service/send-results?project_id=$PROJECT_ID" \
    -H 'Content-Type: multipart/form-data' \
    -H "X-CSRF-TOKEN: $CRSF_ACCESS_TOKEN_VALUE" \
    -b cookiesFile $1 -ik
}

# splitting files to avoid 'Argument list too long' error for curl command
for((i=0; i < ${#FILES_TO_SEND[@]}; i+=BATCH_SIZE))
do
  part=( "${FILES_TO_SEND[@]:i:BATCH_SIZE}" )
  prepareFiles "${part[@]}"
  sendResult "$FILES"
done

#If you want to generate reports on demand use the endpoint `GET /generate-report` and disable the Automatic Execution >> `CHECK_RESULTS_EVERY_SECONDS: NONE`
echo "------------------GENERATE-REPORT------------------"
EXECUTION_NAME="$TEST_ENV%3A%3A$TEST_TAGS%3A%3A$TRIGGER_APP"
EXECUTION_FROM="$TEST_ENV"
EXECUTION_TYPE="$TEST_TAGS"

RESPONSE=$(curl -X GET "$ALLURE_SERVER/allure-docker-service/generate-report?project_id=$PROJECT_ID&execution_name=$EXECUTION_NAME&execution_from=$EXECUTION_FROM&execution_type=$EXECUTION_TYPE" -H "X-CSRF-TOKEN: $CRSF_ACCESS_TOKEN_VALUE" -b cookiesFile)
ALLURE_REPORT=$(grep -o '"report_url":"[^"]*' <<<"$RESPONSE" | grep -o '[^"]*$')
echo "$ALLURE_REPORT"

echo "------------------SEND_TELEGRAM_NOTIFICATION------------------"
sed -i -e "s/__TOKEN__/$TELEGRAM_BOT_TOKEN/g" ./src/main/java/config/allure_telegram_config.json
sed -i -e "s/__CHAT__/$TELEGRAM_CHAT_ID/g" ./src/main/java/config/allure_telegram_config.json
java  \
  "-DprojectName=Traceability Core" \
  "-Dconfig.file=./src/main/java/config/allure_telegram_config.json" \
  "-Denv=$TEST_ENV with tags: $TEST_TAGS" \
  "-DreportLink=$ALLURE_REPORT //" \
  -jar ./lib/allure-notifications-3.1.1.jar

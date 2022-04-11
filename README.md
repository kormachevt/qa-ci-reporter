# qa-ci-reporter 🐘

This is a wrapper plugin that provides possibility to upload test results to different reporting systems.

## Features 🎨

- Upload Allure results to the [Allure Docker Service](https://github.com/fescobar/allure-docker-servicen)
- Upload JUnit results to the [TestRail](https://www.gurock.com/testrail/)

## How to use 👣
#### 1. Import the plugin
* build.gradle
```
plugins {
    ...
    id "com.github.kormachevt.qa.ci.reporter.plugin" version "0.1.8"
}
```

* make sure to add these repositories to the settings.gradle
```
pluginManagement {
    repositories {
        ...
        gradlePluginPortal()
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
        }
    }
}
```


#### 2. You want to upload allure results to the Allure Docker Service
* Maybe You also want to send telegram notification with report summary
```
./gradlew publishToAllure \
    --results-dir="build/allure-results/" \
    --allure-dir="build/reports/allure-report/allureReport/" \
    --url="https://allure.somecompany.com/allure-api" \
    --project-id="local" \
    --username="admin" \
    --password="qwerty12345" \
    --project-name="Test Project" \
    --tags="smoke" \
    --env="test" \
    --trigger="nightly build" \
    --send-notification="true" \
    --telegram-bot-token="1234567890:12345678901234567890123456789012345" \
    --telegram-chat-id="-123456789" 
```
* You can disable sending the notification
```
./gradlew publishToAllure \
    --results-dir="build/allure-results/" \
    --allure-dir="build/reports/allure-report/allureReport/" \
    --url="https://allure.somecompany.com/allure-api" \
    --project-id="local" \
    --username="admin" \
    --password="qwerty12345" \
    --project-name="Test Project" \
    --tags="smoke" \
    --env="test" \
    --trigger="nightly build" \
    --send-notification="false" 
```

list of the optional options:
1. --batch-size (default = "300")
1. --max-file-size (default = "10485760". Maximum file size in Bytes that will be sent to the server. 
   Helps if You are unable to increase upload body size of Allure Service reverse proxy)
1. --send-notification (default = "false")
1. --project-name (default = "default")
1. --telegram-bot-token (no default value)
1. --telegram-chat-id (no default value)
1. --trigger (no default value. Any reason to trigger a test run. It will be displayed above the chart instead of the Project Name if provided)

#### 3. You want to upload JUnit results to the TestRail

```
./gradlew publishToTestRail \
    --url="https://somecompany.testrail.io/" \
    --username="username" \
    --password="qwerty12345" \
    --env="dev" \
    --title="Automated Tests" \
    --suite-id="1" \
    --skip-close-run=true
```
list of the optional options:  
1. --skip-close-run (default = false)

## Libraries in use ♻️
1. Heavily modified version of the [testrail-cli](https://github.com/Open-MBEE/testrail-cli) by Open-MBEE. 
   * fixed skipped test reporting 
   * reduced/sliced log length due to TestRail is returning 413 for some extensively logged tests.
2. [allure-notifications](https://github.com/qa-guru/allure-notifications) by qa-guru    

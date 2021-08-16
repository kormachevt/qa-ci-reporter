package com.github.kormachevt.qa.ci.reporter.plugin

import khttp.get
import khttp.post
import khttp.responses.Response
import khttp.structures.cookie.CookieJar
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.internal.impldep.org.apache.http.HttpStatus
import org.json.JSONObject

abstract class ReportToAllureServerTask : DefaultTask() {
    private val defaultBatchSize: String = "300"
    private val defaultMaxFileSize: String = "10485760"

    init {
        description = "Send report to the Allure Report Server"
        group = BasePlugin.BUILD_GROUP
    }

    @get:Input
    @get:Option(option = "results-dir", description = "Folder with allure results. Usually called 'allure-results'")
    abstract val resultsDir: Property<String>

    @get:Input
    @get:Option(option = "url", description = "url is where the Allure Server is deployed")
    abstract val url: Property<String>

    @get:Input
    @get:Option(
        option = "project-id",
        description = "Project ID according to existent projects in your Allure container"
    )
    abstract val projectId: Property<String>

    @get:Input
    @get:Option(option = "username", description = "Allure Server username")
    abstract val username: Property<String>

    @get:Input
    @get:Option(option = "password", description = "Allure Server password")
    abstract val password: Property<String>

    @get:Input
    @get:Option(option = "tags", description = "A comma separated list of test tags")
    abstract val tags: Property<String>

    @get:Input
    @get:Option(option = "env", description = "Environment where tests have been executed")
    abstract val env: Property<String>

    @get:Input
    @get:Option(option = "trigger", description = "Whats triggered the test run")
    @get:Optional
    abstract val trigger: Property<String>

    @get:Input
    @get:Option(option = "batch-size", description = "Number of files in a batch for upload")
    @get:Optional
    abstract val batch: Property<String>

    @get:Input
    @get:Option(
        option = "max-file-size",
        description = "Maximum file size to upload in bytes. Other files will be ignored"
    )
    @get:Optional
    abstract val maxFileSize: Property<String>

    @get:Input
    @get:Option(option = "send-notification", description = "Sends telegram notification if 'true'")
    @get:Optional
    abstract val notificationEnabled: Property<String>

    @get:Input
    @get:Option(option = "project-name", description = "Name of your project")
    @get:Optional
    abstract val projectName: Property<String>

    @get:Input
    @get:Option(option = "telegram-bot-token", description = "Telegram Bot token for sending notification")
    @get:Optional
    abstract val telegramBotToken: Property<String>

    @get:Input
    @get:Option(option = "telegram-chat-id", description = "Telegram Chat id for sending notification")
    @get:Optional
    abstract val telegramChatId: Property<String>

    @TaskAction
    fun publish() {
        val loginResponse = login(username.get(), password.get())
        val csrfToken = loginResponse.cookies.getCookie("csrf_access_token")!!.value.toString()
        val cookies = loginResponse.cookies
        cleanResults(projectId = projectId.get(), csrf = csrfToken, cookies = cookies)
        sendResults(projectId = projectId.get(), csrf = csrfToken, cookies = cookies)
        val reportLink = generateReport(projectId = projectId.get(), csrf = csrfToken, cookies = cookies)
        if (notificationEnabled.getOrElse("false").toBoolean()) {
            if (!telegramBotToken.isPresent || !telegramChatId.isPresent) {
                throw IllegalStateException("Provide full list of telegram credentials")
            }
            sendTelegramNotification(
                projectName = projectName.getOrElse("default"),
                env = "${env.get()}\n <b>Tags:</b> ${tags.get()}",
                reportLink = reportLink,
                botToken = telegramBotToken.get(),
                chatId = telegramChatId.get()
            )
        }
    }

    private fun login(username: String, password: String): Response {
        println("Logging in to Allure Server as $username")
        val response = post(
            url = "${url.get()}/allure-docker-service/login",
            json = JSONObject(mapOf("username" to username, "password" to password))
        )
        handleError(response)
        return response
    }

    private fun cleanResults(projectId: String, csrf: String, cookies: CookieJar) {
        println("Cleaning results (to avoid merging of current and previous runs)")
        val response = get(
            url = "${url.get()}/allure-docker-service/clean-results",
            headers = mapOf("X-CSRF-TOKEN" to csrf),
            cookies = cookies,
            params = mapOf("project_id" to projectId)
        )
        handleError(response)
    }

    private fun sendResults(projectId: String, csrf: String, cookies: CookieJar) {
        println("Sending results")
        getChunkedFileJSONs(
            resultsDir = resultsDir.get(),
            batch = batch.getOrElse(defaultBatchSize).toInt(),
            maxFileSize = maxFileSize.getOrElse(defaultMaxFileSize).toLong()
        ).forEach {
            val response = post(
                url = "${url.get()}/allure-docker-service/send-results",
                headers = mapOf("X-CSRF-TOKEN" to csrf),
                cookies = cookies,
                params = mapOf("project_id" to projectId),
                json = it
            )
            handleError(response)
        }
    }

    private fun generateReport(projectId: String, csrf: String, cookies: CookieJar): String {
        println("Generating Report")
        val response = get(
            url = "${url.get()}/allure-docker-service/generate-report",
            headers = mapOf("X-CSRF-TOKEN" to csrf),
            cookies = cookies,
            params = mapOf(
                "project_id" to projectId,
                "execution_name" to "${env.get()}::${tags.get()}::${trigger.getOrElse("default")}",
                "execution_from" to env.get(),
                "execution_type" to tags.get()
            )
        )
        handleError(response)
        val url = JSONObject(response.text).getJSONObject("data")["report_url"]
        println("REPORT_URL: $url")
        return url.toString()
    }

    private fun sendTelegramNotification(
        env: String,
        reportLink: String,
        botToken: String,
        chatId: String,
        projectName: String
    ) {
        println("Sending Telegram notification")
        val configPath = getNotificationConfig(
            projectName = if (trigger.isPresent) trigger.get() else projectName,
            chatId = chatId,
            botToken = botToken,
        )
        System.setProperty("projectName", projectName)
        System.setProperty("env", env)
        System.setProperty("config.file", configPath)
        System.setProperty("reportLink", "$reportLink //") // https://github.com/qa-guru/allure-notifications/issues/61
        guru.qa.allure.notifications.Application.main(arrayOf(""))
    }

    private fun handleError(response: Response) {
        if (response.statusCode != HttpStatus.SC_OK) {
            println("Status code: " + response.statusCode)
            println("Headers: " + response.headers)
            println("Response: " + response.text)
            throw IllegalStateException("Error during report sending")
        }
    }
}

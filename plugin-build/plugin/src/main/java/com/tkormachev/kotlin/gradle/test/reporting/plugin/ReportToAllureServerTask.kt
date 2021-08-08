package com.tkormachev.kotlin.gradle.test.reporting.plugin

import khttp.extensions.fileLike
import khttp.get
import khttp.post
import khttp.responses.Response
import khttp.structures.cookie.CookieJar
import org.apache.http.HttpStatus
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


abstract class ReportToAllureServerTask : DefaultTask() {
    private val defaultBatchSize: String = "300"
    private val telegramConfigTokenPlaceholder: String = "__TOKEN__"
    private val telegramConfigChatPlaceholder: String = "__CHAT__"
    private val telegramConfigProjectNamePlaceholder: String = "__NAME__"
    private val notificationsConfigFolder: String = "./build/allure-notification-config"
    private val notificationsConfigName: String = "allure_notification_config.json"

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
                env = env.get(),
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
        getChunkedFileJSONs().forEach {
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
        val configPath = getNotificationConfig(projectName = projectName, chatId = chatId, botToken = botToken)
        System.setProperty("projectName", projectName)
        System.setProperty("env", env)
        System.setProperty("config.file", configPath)
        System.setProperty("reportLink", "$reportLink //")
        guru.qa.allure.notifications.Application.main(arrayOf(""))
    }

    private fun getNotificationConfig(botToken: String, chatId: String, projectName: String): String {
        val config = File("$notificationsConfigFolder/$notificationsConfigName")
        val text = Thread.currentThread().contextClassLoader.getResource(notificationsConfigName)!!.readText()
        if(File(notificationsConfigFolder).exists()) File(notificationsConfigFolder).deleteRecursively()
        if (!File(notificationsConfigFolder).mkdir()) {
            throw IllegalStateException("Unable to crate folder for Allure Notification config")
        }
        if (config.createNewFile()) {
            config.writeText(text)
            updateJsonConfig(file = config, botToken = botToken, chatId = chatId, projectName = projectName)
        } else {
            throw IllegalStateException("Unable to copy Allure Notification config")
        }
        return "$notificationsConfigFolder/$notificationsConfigName"
    }

    private fun updateJsonConfig(file: File, botToken: String, chatId: String, projectName: String) {
        var text = file.readText()
        text = text.replace(telegramConfigTokenPlaceholder, botToken)
            .replace(telegramConfigChatPlaceholder, chatId)
            .replace(telegramConfigProjectNamePlaceholder, projectName)
        file.writeText(text)
    }

    private fun getChunkedFileJSONs(): List<JSONObject> {
        val chunksOfFiles = File(resultsDir.get()).walk()
            .toList()
            .drop(1)
            .map { it.fileLike() }
            .chunked(batch.getOrElse(defaultBatchSize).toInt())
        val resultsList = ArrayList<JSONObject>()
        chunksOfFiles.forEach { it ->
            val array = JSONArray()
            it.forEach {
                val item = JSONObject()
                item.put("file_name", it.name)
                item.put("content_base64", convertToBase64(it.contents))
                array.put(item)
            }
            resultsList.add(JSONObject().put("results", array))
        }
        return resultsList
    }

    private fun convertToBase64(attachment: ByteArray): String {
        return Base64.getEncoder().encodeToString(attachment)
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

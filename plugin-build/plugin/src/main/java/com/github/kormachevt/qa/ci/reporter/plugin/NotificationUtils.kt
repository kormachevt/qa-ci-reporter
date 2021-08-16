package com.github.kormachevt.qa.ci.reporter.plugin

import java.io.File

private const val CONFIG_TOKEN_PLACEHOLDER: String = "__TOKEN__"
private const val CONFIG_CHAT_PLACEHOLDER: String = "__CHAT__"
private const val PROJECT_NAME_PLACEHOLDER: String = "__NAME__"
private const val CONFIG_DIR: String = "./build/allure-notification-config"
private const val CONFIG_NAME: String = "allure_notification_config.json"

fun getNotificationConfig(botToken: String, chatId: String, projectName: String): String {
    val config = File("$CONFIG_DIR/$CONFIG_NAME")
    val text = Thread.currentThread().contextClassLoader.getResource(CONFIG_NAME)!!.readText()
    if (File(CONFIG_DIR).exists()) File(CONFIG_DIR).deleteRecursively()
    if (!File(CONFIG_DIR).mkdir()) {
        throw IllegalStateException("Unable to create folder for Allure Notification config")
    }
    if (config.createNewFile()) {
        config.writeText(text)
        updateJsonConfig(
            file = config,
            botToken = botToken,
            chatId = chatId,
            projectName = projectName
        )
    } else {
        throw IllegalStateException("Unable to copy Allure Notification config")
    }
    return "$CONFIG_DIR/$CONFIG_NAME"
}

private fun updateJsonConfig(file: File, botToken: String, chatId: String, projectName: String) {
    var text = file.readText()
    text = text.replace(CONFIG_TOKEN_PLACEHOLDER, botToken)
        .replace(CONFIG_CHAT_PLACEHOLDER, chatId)
        .replace(PROJECT_NAME_PLACEHOLDER, projectName)
    file.writeText(text)
}

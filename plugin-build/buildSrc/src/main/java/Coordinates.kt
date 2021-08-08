object PluginCoordinates {
    const val ID = "com.tkormachev.kotlin.gradle.qa.reporting.plugin"
    const val GROUP = "com.tkormachev.kotlin.gradle.qa"
    const val VERSION = "0.1.0"
    const val IMPLEMENTATION_CLASS = "com.tkormachev.kotlin.gradle.qa.reporting.plugin.ReportingPlugin"
}

object PluginBundle {
    const val VCS = "https://github.com/kormachevt/test-ci-reporter"
    const val WEBSITE = "https://github.com/kormachevt/test-ci-reporter"
    const val DESCRIPTION = "Reporting plugin for Allure results"
    const val DISPLAY_NAME = "Reporting plugin for Allure results"
    val TAGS = listOf(
        "plugin",
        "gradle",
        "reporting"
    )
}


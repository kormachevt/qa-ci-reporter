object PluginCoordinates {
    const val ID = "com.github.kormachevt.qa.ci.reporter.plugin"
    const val GROUP = "com.github.kormachevt"
    const val VERSION = "0.1.4"
    const val IMPLEMENTATION_CLASS = "com.github.kormachevt.qa.ci.reporter.plugin.ReportingPlugin"
}

object PluginBundle {
    const val VCS = "https://github.com/kormachevt/qa-ci-reporter"
    const val WEBSITE = "https://github.com/kormachevt/qa-ci-reporter"
    const val DESCRIPTION = "Plugin for publishing test reports to remote servers"
    const val DISPLAY_NAME = "qa-ci-reporter"
    val TAGS = listOf(
        "plugin",
        "gradle",
        "reporting"
    )
}


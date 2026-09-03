package com.lagradost.cloudstream3.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File
import java.nio.charset.StandardCharsets

@CacheableTask
abstract class DeployWithAdbTask : DefaultTask() {

    @get:Input
    @set:Option(option = "wait-for-debugger", description = "Enables debugging flag when starting the discord activity")
    var waitForDebugger: Boolean = false

    @get:Input abstract val adbPath: Property<String>
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val pluginFile: RegularFileProperty

    @TaskAction
    fun deployWithAdb() {
        val adbPath = adbPath.get()
        val process = ProcessBuilder(adbPath, "start-server")
            .redirectErrorStream(true)
            .start()
        process.waitFor()

        val devicesProcess = ProcessBuilder(adbPath, "devices")
            .redirectErrorStream(true)
            .start()
        val devicesOutput = devicesProcess.inputStream.bufferedReader().readText()
        devicesProcess.waitFor()

        val devices = devicesOutput.lines()
            .filter { it.contains("\tdevice") }
            .map { it.split("\t").first() }

        require(devices.size == 1) {
            "Only one ADB device should be connected, but ${devices.size} were!"
        }

        val file: File = pluginFile.get().asFile
        val path = "/storage/emulated/0/Cloudstream3/plugins/"
        val device = devices[0]

        // Push file
        ProcessBuilder(adbPath, "-s", device, "push", file.absolutePath, path + file.name)
            .start().waitFor()

        // Make file readonly for newer Android versions
        ProcessBuilder(adbPath, "-s", device, "shell", "chmod", "-w", path + file.name)
            .start().waitFor()

        // Launch CloudStream
        val args = mutableListOf("start", "-a", "android.intent.action.VIEW", "-d", "cloudstreamapp:")
        if (waitForDebugger) args.add("-D")

        val launchProcess = ProcessBuilder(adbPath, "-s", device, "shell", "am", *args.toTypedArray())
            .start()
        val response = String(launchProcess.inputStream.readAllBytes(), StandardCharsets.UTF_8)
        launchProcess.waitFor()

        if (response.contains("Error")) logger.error(response)
        logger.lifecycle("Deployed $file to $device")
    }
}

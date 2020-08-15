package modules.loops.pageChecking

import api.VkPlatform
import chatIds
import log
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Paths
import modules.loops.Loop
import modules.sendGet

class PageChecker : Loop {
    override val delay = 60000L
    override val name = "Проверка обновления страницы"

    private val pages = listOf<Link>()

    private val vk = VkPlatform

    override fun call() {
        for (page in pages) {
            if (isUpdated(page.trueUrl) == true) {
                log.info("Page ${page.showingUrl} was updated")
                chatIds.forEach { vk.send("Страница ${page.showingUrl} была обновлена", it) }
            }
        }
    }

    private fun getPath(page: String): String {
        val filePath = "/data/savedPages/" + page.replace(Regex("""[\\?|"/.:<>*]"""), "_") + ".txt"
        return Paths.get("").toAbsolutePath().toString() + filePath
    }

    private fun isUpdated(page: String): Boolean? {
        val path = getPath(page)
        val newPage = sendGet(page) ?: return null
        return try {
            if (newPage != File(path).readText()) {
                File(path).bufferedWriter().use { it.write(newPage) }
                log.info("Page $path was updated")
                true
            } else {
                false
            }
        } catch (e: FileNotFoundException) {
            log.warning("File $path not found")
            File(path).writeText(newPage)
            log.info("File $path was created")
            null
        }
    }
}

data class Link(val name: String, val trueUrl: String, val showingUrl: String = trueUrl)
package chatbot.chatModules

import api.IntegerNumber
import api.TextMessageParser
import api.VkApi
import chatbot.CommandPermission
import chatbot.ModuleObject
import chatbot.OnCommand
import chatbot.chatBotEvents.LongPollNewMessageEvent
import database.ScheduleTable
import database.dbQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.time.ZoneId
import java.util.*

@ModuleObject
object Schedule {
    // todo: add leap year support
    private val parser = TextMessageParser()
    private val dataRegex = Regex("^(\\d\\d*).(\\d\\d*)\$")
    private val monthDays = mutableListOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    @OnCommand(["добавитьсобытие", "добавитьрасписание"], "добавляет ивент в расписание")
    fun add(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val (daysFromYearStart, text) = parse(event.text, chatId, api) ?: return

        val id = dbQuery {
            ScheduleTable.insert {
                it[this.chatId] = chatId
                it[this.days] = daysFromYearStart
                it[this.text] = text
            }[ScheduleTable.id]
        }

        api.send("Событие создано. Его ID $id", chatId)
    }

    @OnCommand(["удалитьсобытие", "удалитьрасписание"], "удаляет событие из расписания", CommandPermission.ADMIN)
    fun delete(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId

        val parsed = parser.parse(event.text)
        val scheduleId = parsed.get<IntegerNumber>(1)?.number?.toInt()
        if (scheduleId == null) {
            api.send("Не указан ID", chatId)
            return
        }

        val countDeleted = dbQuery {
            ScheduleTable.deleteWhere {
                (ScheduleTable.id eq scheduleId) and (ScheduleTable.chatId eq chatId)
            }
        }

        if (countDeleted == 0) {
            api.send("Указанный id не найден", chatId)
            return
        }

        api.send("Удалено", chatId)
    }

    @OnCommand(["расписание"], "показывает все расписания")
    fun list(event: LongPollNewMessageEvent) {
        val api = event.api
        val chatId = event.chatId
        val dateNow = dayNow()

        val tasks = dbQuery {
            ScheduleTable.select {
                (ScheduleTable.chatId eq chatId) and (ScheduleTable.days greaterEq dateNow)
            }.map { it[ScheduleTable.text] to it[ScheduleTable.days] }
        }

        if (tasks.isEmpty()) {
            api.send("Расписание пусто", chatId)
            return
        }

        val toSend = buildString {
            for ((text, date) in tasks) {
                append(text)
                append(" -> ")
                append(toPrettyDate(date))
                appendLine()
            }
        }

        api.send(toSend, chatId)
    }

    fun onDay(api: VkApi, dayNow: Int) {
        val scheduleToday = dbQuery {
            ScheduleTable.select {
                (ScheduleTable.days eq dayNow)
            }.map { it[ScheduleTable.chatId] to it[ScheduleTable.text] }
        }
        if (scheduleToday.isEmpty()) return

        val todayTasks = mutableMapOf<Int, StringBuilder>()
        val reportPreamble = "Задачи на сегодня:\n"
        for ((chatId, text) in scheduleToday) {
            if (todayTasks[chatId] == null) {
                todayTasks[chatId] = StringBuilder(reportPreamble).apply {
                    appendLine(text)
                    appendLine()
                }
            } else {
                todayTasks[chatId]!!.appendLine(text).appendLine()
            }
        }

        for ((chatId, text) in todayTasks) {
            api.send(text.toString(), chatId)
        }

    }

    fun dayNow(): Int {
        var dayNow = 0
        val dateNow = Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        dayNow += monthDays[dateNow.month.ordinal]
        dayNow += dateNow.dayOfMonth

        return dayNow
    }

    private fun parse(text: String, chatId: Int, api: VkApi): Pair<Int, String>? {
        val textLines = text.lines()
        if (textLines.size < 3) {
            api.send("Некорректное число аргументов. Синтаксис команды:\n /добавитьРасписание\nДД.ММ\nТекст", chatId)
            return null
        }
        val date = dataRegex.find(textLines[1])?.groupValues
        if (date == null || date.size != 3) {
            api.send("Некорректная дата. Она должна быть в формате ДД.ММ, например, 01.04", chatId)
            return null
        }

        val day = date[1].toInt()
        val month = date[2].toInt()
        var dayFromYearStart = 0
        for (i in 0 until month - 1) {
            dayFromYearStart += monthDays[i]
        }

        dayFromYearStart += day

        val scheduleTaskText = textLines.subList(2, textLines.size).joinToString("\n")
        return dayFromYearStart to scheduleTaskText
    }

    private fun toPrettyDate(_days: Int): String {
        var days = _days
        var month = 0
        for ((i, daysCount) in monthDays.withIndex()) {
            if (days < daysCount) {
                month = i + 1
                break
            }
            days -= daysCount
        }

        return "${"%02d".format(month)}:${"%02d".format(days)}"
    }
}
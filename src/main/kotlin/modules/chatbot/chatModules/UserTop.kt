package modules.chatbot.chatModules

import api.IntegerNumber
import api.TextMessageParser
import database.UserScore
import database.dbQuery
import kotlinx.coroutines.selects.select
import modules.chatbot.CommandPermission
import modules.chatbot.ModuleObject
import modules.chatbot.OnCommand
import modules.chatbot.Permissions
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select

@ModuleObject
object UserTop {
    @OnCommand(["топ", "top"], "выводит топ пользователей по e-баллам", CommandPermission.ADMIN)
    fun top(event: LongPollNewMessageEvent) {
        val chatId = event.chatId
        val api = event.api
        val parser = TextMessageParser(event.platform).parse(event.text)
        val count = parser.get<IntegerNumber>(1)?.number?.toInt() ?: 10

        val text = StringBuilder("Вот топ пользователей по e-баллам в этом чате:\n")

        dbQuery {
            UserScore
                    .select { UserScore.chatId eq chatId }
                    .orderBy(UserScore.score, SortOrder.DESC)
                    .limit(count)
                    .forEachIndexed { index, resultRow ->
                        val n = index + 1
                        val screenName = api.getUserNameById(resultRow[UserScore.userId])
                        val showingScore = RatingSystem.calculateShowingScore(resultRow[UserScore.score])
                        if (screenName != null) {
                            text.append("$n. @$screenName [$showingScore]\n")
                        }
                    }
        }

        api.send(text.toString(), chatId)
    }
}
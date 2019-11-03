package modules.IcsEvents

class Event(val name: String, val dateStart: Long, val dateEnd: Long, _category: String){
    var category = when(_category) {
        "Политология" -> "\uD83D\uDC51Политология"
        "ИнЯз/Базовый курс 1" -> "\uD83C\uDDEC\uD83C\uDDE7ИнЯз/Базовый курс"
        else -> _category
    }
}
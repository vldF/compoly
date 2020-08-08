package api.keyboards

class KeyboardBuilder {
    private val buttons = mutableListOf<KeyboardButton>()

    fun addButton(button: KeyboardButton): KeyboardBuilder {
        buttons.add(button)
        return this
    }

    fun build() = Keyboard(buttons)
}
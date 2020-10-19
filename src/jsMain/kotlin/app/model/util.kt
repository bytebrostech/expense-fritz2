package app.model

import kotlin.math.absoluteValue

fun Float.asMoney(): String = this.toString().let {
    val sign = if (this.absoluteValue < 0) "-" else ""
    if (it.tooManyDecimals()) {
        this.toString().substring(0, it.indexOfDecimal() + 2)
    } else if (it.indexOfDecimal() == -1) {
        "$sign\$${this.absoluteValue}.00"
    } else {
        when(it.trailingDigits()) {
            0 -> "$sign\$${this.absoluteValue}00"
            1 -> "$sign\$${this.absoluteValue}0"
            else -> "$sign\$${this.absoluteValue}"
        }
    }
}
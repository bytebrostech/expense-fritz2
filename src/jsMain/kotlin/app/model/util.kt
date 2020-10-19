package app.model

fun Float.asMoney(): String = this.toString().let {
    if (it.tooManyDecimals()) {
        this.toString().substring(0, it.indexOfDecimal() + 2)
    } else this.toString()
}
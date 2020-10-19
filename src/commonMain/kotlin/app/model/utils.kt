package app.model

//expect fun Float.asMoney(): String
fun String.indexOfDecimal() = this.indexOf('.')
fun String.trailingDigits() = (this.length - 1 - this.indexOfDecimal())
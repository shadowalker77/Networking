package ir.ayantech.ayannetworking.helper

fun List<Int>.dePent(feed: Array<Int>): String {
    return String(this.mapIndexed { index, i ->
        (i / feed[index % feed.size]).toChar()
    }.toCharArray())
}
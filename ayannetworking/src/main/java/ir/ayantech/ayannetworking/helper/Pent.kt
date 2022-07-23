package ir.ayantech.ayannetworking.helper

fun List<Int>.dePent(feed: Array<Int>?): String {
    return if (feed == null) {
        String(this.map { it.toChar() }.toCharArray())
    } else {
        String(this.mapIndexed { index, i ->
            (i / feed[index % feed.size]).toChar()
        }.toCharArray())
    }
}
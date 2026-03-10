fun main() {
    print("Please enter a number: ")
    val input = readln()
    val inputAsInteger = input.toIntOrNull()


    val output = when (inputAsInteger) {
        null -> "Enter a valid number!"
        3 -> "The number is three."
        5 -> "The number is five."
        in 10..20 -> "The number is between 10 and 20."
        else -> "..."
    }

    println(output)
}
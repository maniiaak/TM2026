fun main() {
    print("Please enter a number: ")
    val input = readln()
    val inputAsInteger = input.toIntOrNull()

    if(inputAsInteger != null) {
        val output = if(inputAsInteger % 2 == 0) "even" else "odd"
        println(output)

        val isEven = inputAsInteger % 2 == 0

        if(isEven) {
            println("The number is even.")
        } else if(!isEven) {
            println("The number is odd.")
        }
    } else {
        println("Enter a valid number!")
    }
}
fun main() {
    print("Please enter a number: ")
    val input = readln()
    val inputAsInteger = input.toIntOrNull()?.rem(2)?.equals(0)

    println("Is even? $inputAsInteger")
}
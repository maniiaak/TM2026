fun main() {
    val x = 11
    val y = 9
    val z = 9
    println(x == y)
    println(x > y)
    println(y >= z)

    val areNumbersTheSame = x == y
    println(areNumbersTheSame)

    println("Is x an even number? ${x % 2 == 0}")
}
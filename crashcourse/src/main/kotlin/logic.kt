fun main() {
    val x = 12
    val y = 11

    val areBothEven = x % 2 == 0 && y % 2 == 0
    println("Are x and y even? $areBothEven")

    val isOneEven = x % 2 == 0 || y % 2 == 0
    println("Is x or y even? $isOneEven")

    val priorityTest = x % 2 == 0 || y % 2 == 0 && x + y == 25
    println(priorityTest)
}

// && -> and
// || -> or
// "and" is prioritized over "or"
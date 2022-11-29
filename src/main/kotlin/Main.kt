package calculator
import java.util.*

val minusRegex = Regex("-*")             // checks if token is -
val plusRegex = Regex("\\+*")            // checks if token is +
val mutDivRegex = Regex("[*/]+")         // check if token is * or /
val numberRegex = Regex("-?\\d+")        // checks if the token is a number
val alphaNumRegex = Regex("-?\\w+")      // checks if token is a variable or integer
val commandRegex = Regex("/\\w*")        // checks if inputs are commands
val variableRegex = Regex(".*=.*")       // checks if inputs are variables
val onlyAlphaRegex = Regex("[a-zA-Z]+")  // checks if token is variable

val getVariable: (String) -> String? = { variable -> variables[variable]}
val setVariable = {variable: String, value: String -> variables[variable] = value}

var variables = emptyMap<String, String>().toMutableMap()

fun main() {
    println("Input values with spaces between each symbols   example: 23 + (21 * 2)")
    println("Variables can be defined   example: a = 5")
    println("Input /exit to end run")

    loop@while (true) {
        val input = readLine()!!
        if (input.matches(commandRegex)) { // checking if a command was entered
            when (input) {
                "/exit" -> break@loop // command to end the run of the program
                "/help" -> println("The program calculates the accumulation of these numbers")
                else -> println("Unknown command")
            }
        } else if(input.matches(variableRegex)) {
            makeVariable(input)
        } else if (input.isBlank()) {
            // do nothing
        } else if( input.trim().matches(onlyAlphaRegex)) {
            println(getVariable(input.trim()) ?: "Unknown variable") // if variable not found null will be returned, returning unknown variable
        } else {
            accumulation((input))
        }
    }
    println("Bye!")
}
// places the variable into the map variables to hold it
fun makeVariable(input: String) {
    val variable = input.replace("=", " ")
    val scanner = Scanner(variable)
    val token = scanner.next()
    val num = scanner.next()

    if (scanner.hasNext()) { // signals that the variable had excess parts
        println("Invalid assignment")
        return
    }
    if (token.matches(onlyAlphaRegex)) {
        try {
            if (num.matches(numberRegex)) {
                setVariable(token, num)
            } else {
                setVariable(token, getVariable(num)!!)
            }
        } catch(e: Exception) {
            println("Invalid assignment")
            return
        }
    } else {
        println("Invalid identifier")
        return
    }
}

fun accumulation(input: String) {
    val processedInput: String
    val postfixList: MutableList<String>
    val result: String

    try {
        processedInput = processString(input)
        postfixList = infixToPostfix(processedInput)
        result = postfixAccumulation(postfixList)
    } catch (e:java.lang.Exception) {
        println("Invalid Expression")
        return
    }
    println(result)
}

fun infixToPostfix(str: String): MutableList<String> {
    var tokenWeight: Int
    var stackWeight: Int
    val scanner = Scanner(str)
    val outList = emptyList<String>().toMutableList()
    val stack = ArrayDeque<String>()
    while(scanner.hasNext()) {
        val token = operatorEval(scanner.next()) // can return Invalid Expression
        if (token == "Invalid Expression") { // if OperatorEval returns Invalid Expression exception is thrown
            throw Exception("Invalid Expression")
        } else if (token.matches(alphaNumRegex)) { // checks for numbers and variables
            if (token.matches(onlyAlphaRegex)) {
                outList.add(getVariable(token)!!)
            } else {
                outList.add(token)
            }

        } else {
            tokenWeight = weightValue(token)
            stackWeight = weightValue(stack.lastOrNull())
            if (stack.isEmpty() || stack.last() == "(" || token == "(") {
                stack.add(token)
            }  else if(token == ")") {
                while (stack.last() != "(") {
                    outList.add(stack.last())
                    try { // if the end on the stack is reached without encountering an operator (
                        stack.removeLast()
                    } catch (e: Exception) {
                        throw Exception("Invalid Expression")
                    }
                }
                stack.removeLast()
            } else if(tokenWeight > stackWeight) {
                stack.add(token)
            } else {
                while (stack.isNotEmpty() && tokenWeight <= weightValue(stack.last())) {
                    outList.add(stack.last())
                    stack.removeLast()
                }
                stack.add(token)
            }
        }
    }
    while (stack.isNotEmpty()) {
        outList.add(stack.last())
        stack.removeLast()
        if (stack.lastOrNull() == "(") { // if an operator ( still remains in the stack it means there was no operator )
            throw Exception("Invalid Expression")
        }
    }
    return outList
}
// set the precedent of the operator
fun weightValue(token:String?): Int {
    return when (token) {
        "+" -> 1
        "-" -> 1
        "*" -> 2
        "/" -> 2
        else -> 0
    }
}
// compresses the operator to a single symbol or returns invalid expression if there are multiple * or / in a row
fun operatorEval(operator: String): String {
    if (operator.length == 1) {
        return operator
    } else if (operator.matches(minusRegex)) {
        return if (operator.length % 2 == 0) { // determines if the combination will result in a positive sum
            "+"
        } else {
            "-"
        }
    } else if (operator.matches(plusRegex)) { // compresses number of +
        return "+"
    } else if (operator.matches(mutDivRegex)) {
        return "Invalid Expression"
    }
    return operator
}
// breaks the string apart placing spaces between all none like operators and numbers
fun processString(str: String): String {
    val strTrim = str.trim()
    val multiDivCheckRegex = Regex(".*[/*]{2,}.*")
    if (strTrim.matches(multiDivCheckRegex)) { // escapes before any farther processing is preformed if operator is invalid
        throw Exception("Invalid Expression")
    }

    val numberGrabRegex = Regex("\\(?\\W+\\)\\?") // this will make a list of only numbers
    val operatorGrabRegex = Regex("[^*/+-]+") // this will make a list of only operators
    val numList = strTrim.split(numberGrabRegex) // list with only the numbers from the string
    val operatorList = strTrim.split(operatorGrabRegex) // list with only the operators from the list
    val mergeList = emptyList<String>().toMutableList() // list to merge numList and operatorList into
    for ( i in numList.indices) {
        mergeList.add(operatorList[i])
        mergeList.add(numList[i])
    }
    return mergeList.joinToString(" ").replace("(", " ( ").replace(")", " ) ")
}

// accumulates list in postfix form
fun postfixAccumulation(postfixList: MutableList<String>): String {
    var stack = ArrayDeque<String>()
    for(token in postfixList) {
        if (token.matches(alphaNumRegex)) {
            stack.add(token)
        } else {
            stack = operation(token, stack)
        }
    }
    return stack.removeLast()
}
// operates on the stack based on given operator
fun operation(operator: String, stack: ArrayDeque<String>): ArrayDeque<String> {
    val num1 = stack.removeLast().toBigInteger()
    val num2 = stack.removeLast().toBigInteger()
    when(operator) {
        "+" -> stack.add((num2 + num1).toString())
        "-" -> stack.add((num2 - num1).toString())
        "*" -> stack.add((num2 * num1).toString())
        "/" -> stack.add((num2 / num1).toString())
    }
    return stack
}

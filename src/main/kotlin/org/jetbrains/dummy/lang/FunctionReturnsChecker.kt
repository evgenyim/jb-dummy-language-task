package org.jetbrains.dummy.lang

import org.jetbrains.dummy.lang.tree.*

class FunctionReturnsChecker(private val reporter: DiagnosticReporter) : AbstractChecker() {
    val m: MutableMap<String, Boolean> = mutableMapOf()
    override fun inspect(file: File) {
        for (func in file.functions) {
            m[func.name] = functionReturns(func.body)
        }
        for (func in file.functions) {
            inspectBlock(func.body)
        }
    }

    private fun functionReturns(block: Block): Boolean {
        for (statement in block.statements) {
            if (statement is ReturnStatement && statement.result != null) {
                return true
            } else if (statement is ReturnStatement) {
                return false
            }
        }
        return false
    }

    private fun inspectBlock(block: Block) {
        for (statement in block.statements) {
            if (statement is Assignment) {
                checkFunctionCall(statement.rhs)
            }
            if (statement is VariableDeclaration) {
                checkFunctionCall(statement.initializer)
            }
            if (statement is IfStatement) {
                checkFunctionCall(statement.condition)
                inspectBlock(statement.thenBlock)
                if (statement.elseBlock != null) inspectBlock(statement.elseBlock)
            }
            if (statement is ReturnStatement) {
                checkFunctionCall(statement.result)
                break
            }
            if (statement is FunctionCall) {
                for (arg in statement.arguments) {
                    checkFunctionCall(arg)
                }
            }
        }
    }

    private fun checkFunctionCall(expr: Expression?) {
        if (expr is FunctionCall && m[expr.function] == false)  {
            reportFunctionDoesnotReturn(expr)
            for (arg in expr.arguments) {
                checkFunctionCall(arg)
            }
        }
    }

    // Use this method for reporting errors
    private fun reportFunctionDoesnotReturn(function: FunctionCall) {
        reporter.report(function, "Function '${function.function}' doesn't return value")
    }
}
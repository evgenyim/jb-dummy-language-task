package org.jetbrains.dummy.lang

import org.jetbrains.dummy.lang.tree.*

class VariableInitializationChecker(private val reporter: DiagnosticReporter) : AbstractChecker() {
    override fun inspect(file: File) {
        for (func in file.functions) {
            inspectFunction(func)
        }
    }

    private fun inspectFunction(func: FunctionDeclaration) {
        val m: MutableMap<String, Boolean> = mutableMapOf()
        for (param in func.parameters) m[param] = true
        inspectBlock(m, func.body)

    }

    private fun inspectBlock(initialized: Map<String, Boolean>, block: Block): Map<String, Boolean> {
        var innerMap: MutableMap<String, Boolean> = initialized.toMutableMap()
        for (statement in block.statements) {
            if (statement is Assignment) {
                if (statement.rhs is FunctionCall) {
                    checkFunctionCall(innerMap, statement.rhs)
                }
                if (innerMap.containsKey(statement.variable)) innerMap[statement.variable] = true
                continue
            }
            if (statement is VariableDeclaration) {
                if (statement.initializer == null) {
                    innerMap[statement.name] = false
                } else if (isInitialized(innerMap, statement.initializer)) {
                    innerMap[statement.name] = true
                }
                continue
            }
            if (statement is FunctionCall) {
                checkFunctionCall(innerMap, statement)
                continue
            }
            if (statement is IfStatement) {
                if (statement.condition is VariableAccess) {
                    isInitialized(innerMap, statement.condition)
                } else if (statement.condition is FunctionCall) {
                    checkFunctionCall(innerMap, statement.condition)
                }
                if (statement.condition is BooleanConst && statement.condition.value) {
                    innerMap = appendMap(innerMap, inspectBlock(innerMap, statement.thenBlock))
                } else if (statement.condition is BooleanConst && !statement.condition.value &&
                    statement.elseBlock != null) {
                    innerMap = appendMap(innerMap, inspectBlock(innerMap, statement.elseBlock))
                } else if (statement.elseBlock == null) {
                    inspectBlock(innerMap, statement.thenBlock)
                } else {
                    val thenMap = inspectBlock(innerMap, statement.thenBlock)
                    val elseMap = inspectBlock(innerMap, statement.elseBlock)
                    val mergedMap = mergeMaps(thenMap, elseMap)
                    innerMap = appendMap(innerMap, mergedMap)
                }
                continue
            }
            // If return something, then we don't need to check anymore
            if (statement is ReturnStatement && statement.result != null) {
                isInitialized(innerMap, statement.result)
                break
            }
        }
        return innerMap

    }

    private fun mergeMaps(map1: Map<String, Boolean>, map2: Map<String, Boolean>): MutableMap<String, Boolean> {
        val retMap: MutableMap<String, Boolean> = mutableMapOf()
        for (key in map1.keys) {
            if (map2.containsKey(key) && map1[key] == true && map2[key] == true) retMap[key] = true
        }
        return retMap
    }

    private fun appendMap(map1: Map<String, Boolean>, map2: Map<String, Boolean>): MutableMap<String, Boolean> {
        val retMap = map1.toMutableMap()
        for (key in map2.keys) {
            if (!retMap.containsKey(key)) {
                retMap[key] = map2[key] ?: false
            } else if (retMap[key] == false && map2[key] == true) {
                retMap[key] = true
            }
        }
        return retMap
    }

    private fun isInitialized(initialized: Map<String, Boolean>, expr: Expression): Boolean {
        if (expr is VariableAccess) {
            return if (initialized.containsKey(expr.name) && initialized[expr.name] == true) {
                true
            } else {
                reportAccessBeforeInitialization(expr)
                false
            }
        } else if (expr is FunctionCall) {
            checkFunctionCall(initialized, expr)
        }
        return true
    }

    private fun checkFunctionCall(initialized: Map<String, Boolean>, func: FunctionCall) {
        for (expr in func.arguments) {
            isInitialized(initialized, expr)
        }
    }

    // Use this method for reporting errors
    private fun reportAccessBeforeInitialization(access: VariableAccess) {
        reporter.report(access, "Variable '${access.name}' is accessed before initialization")
    }
}
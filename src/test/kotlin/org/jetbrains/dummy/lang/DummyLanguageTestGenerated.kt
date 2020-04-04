package org.jetbrains.dummy.lang

import org.junit.Test

class DummyLanguageTestGenerated : AbstractDummyLanguageTest() {
    @Test
    fun testBad() {
        doTest("testData/bad.dummy")
    }
    
    @Test
    fun testGood() {
        doTest("testData/good.dummy")
    }
    
    @Test
    fun testTest1() {
        doTest("testData/test1.dummy")
    }
    
    @Test
    fun testTest2() {
        doTest("testData/test2.dummy")
    }
    
    @Test
    fun testTest3() {
        doTest("testData/test3.dummy")
    }
}

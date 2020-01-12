package com.newmcs.filive.test

import com.newmcs.filive.application.ApplicationListener
import com.newmcs.filive.atomic.AtomicAbstractObject
import com.newmcs.filive.atomic.event.example.AtomicObjectDefaultHandler
import com.newmcs.filive.concurrent.PeriodicObject
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field
import java.util.*
import kotlin.test.assertEquals

class DefaultHandler : ApplicationListener
{
    override fun getHandler(): ApplicationListener = this
}

class SampleObject : PeriodicObject<DefaultHandler>(UUID.randomUUID().toString())
{
    var field1 : String? = null

    var finalValue : Any? = null
    override fun changedListener(field: Field, previous: Any?, after: Any?, handleInstance: Any?)
    {
        println("[TEST] Changed field value: [$handleInstance.${field.name}::$previous -> $after]")
        this.finalValue = after
    }
}

class ChangeListenerTest
{

    @Before
    fun configure()
    {
        AtomicAbstractObject.registerMonitorHandler(SampleObject::class.java, AtomicObjectDefaultHandler())
    }

    @Test
    fun test()
    {
        val myVariable = SampleObject()
        val myHandler = DefaultHandler()
        myVariable.setEnable(myHandler)

        Thread.sleep(1000)
        myVariable.field1 = "Hello world"
        Thread.sleep(100)
        assertEquals(myVariable.finalValue, "Hello world")
    }
}
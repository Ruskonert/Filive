package com.newmcs.filive.test

import com.newmcs.filive.application.ApplicationListener
import com.newmcs.filive.IObject
import com.newmcs.filive.IObjectCollector
import com.newmcs.filive.handler.CollectionHandler
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IObjectCollectorExample : IObjectCollector<IObjectExample>()

class UndefinedHandler : CollectionHandler
{
    class Internal : ApplicationListener
    {
        override fun getHandler(): ApplicationListener {
            return this
        }
    }
    private val internalListener : Internal = Internal()
    override fun getHandler(): ApplicationListener = this.internalListener
}


class IObjectExample : IObject<IObjectExample>()
{
    private val field1 : String = "Hello world"
    private val field2 : Int = 2
    private val field3 : ArrayList<String> = ArrayList()
    var field4 : IObjectExample? = null
    init
    {
        field3.add("One string")
        field3.add("Two string")
        field3.add("Three string")
    }
}

class IObjectTest
{
    @Test
    fun test()
    {
        println("[+] IObject serialization test started")
        val handler = UndefinedHandler()
        val collection = IObjectCollectorExample().create(handler)

        val example = IObjectExample().create()
        val example2 = IObjectExample().create()
        example.field4 = example2
        val result = example.stringify()

        println("original: " + example.stringify(true))
        val refObject = collection.getEntity(example.getUniqueId())

        assertNotNull(result)
        assertNotNull(refObject)

        // It's not recommended to register the deserialized object. This is because
        // the Collector already manages an object with that UUID.
        //
        // Registering more than one other object can cause unexpected results, The code
        // is ONLY designed to verify that deserialization works.
        val deserializeObject = IObjectCollector.deserialize<IObjectExample>(result)
        assertNotNull(deserializeObject)

        println("deserialize from: " + deserializeObject.stringify(true))
        assertEquals(example::class, deserializeObject::class)
        assert(deserializeObject == example)
        println("[+] IObject serialization test successfully!")
    }
}

package com.newmcs.filive.test

import com.newmcs.filive.Standard
import com.newmcs.filive.IObjectCollector
import com.newmcs.filive.IObjectUtil
import com.newmcs.filive.IStorableObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.io.File
import java.lang.reflect.Field
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MyEntityCollector : IObjectCollector<MyEntity>()

class MyEntity : IStorableObject<MyEntity>()
{
    var name : String = "Default Name"
    var health : Float = 100.0f
    var exp : Double = 0.0

    override fun changedListener(field: Field, previous: Any?, after: Any?, handleInstance: Any?)
    {
        super.changedListener(field, previous, after, handleInstance)
        println("[INFO] FIELD_CHANGED: ${field.name}::[${previous}] -> [$after]")
    }
}

class IStorableObjectTest
{
    @Test
    fun test() = runBlocking {
        val coll = MyEntityCollector().create(Standard.STD_HANDLER) as MyEntityCollector
        val entity = MyEntity().create()
        val collector = entity.getIObjectCollector()
        assertEquals(coll, collector)

        @Language("JSON")
        val expected = "{\"name\":\"${entity.name}\",\"health\":100.0,\"exp\":0.0,\"uid\":\"${entity.getUniqueId()}\"}"
        assertEquals(expected, entity.stringify(false))
        assertNotNull(entity.getInspector())

        GlobalScope.launch {
            entity.name = "Changed name"
            entity.health -= 20.0f
            entity.exp = 10000.0
            delay(300L)

            entity.name = "Changed again!"
            entity.health += 16.0f
            entity.exp = 20000.0
            delay(300L)

            entity.name = "Final name"
            entity.health -= 40.0f
            entity.exp -= 1000.0
        }
        println("Running for IStorableObjectTest ...")
        delay(1200L)
        println(entity.stringify(true))

        println("[INFO] Checking IStorableObject file was existed ...")

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val result = Files.exists(entity.getInspector()?.getTargetPath())
        assert(result)

        IObjectUtil.deleteDirectory(File("data/"))
        println("[INFO] IStorableObjectTest is completed successfully!")
    }
}
package com.newmcs.filive.test

import com.newmcs.filive.IObjectCollector
import com.newmcs.filive.Standard
import kotlin.random.Random

object MainTest
{
    private val collector : IObjectCollector<MyEntity> = MyEntityCollector().create(Standard.STD_HANDLER)

    @JvmStatic
    fun main(args: Array<String>)
    {
        val entity = MyEntity().create()
        var count = 0
        while(true)
        {
            if(count == 10) break
            val e = Random.nextFloat()
            Thread.sleep(300L)
            entity.health = e
            count += 1
        }
        return
    }
}
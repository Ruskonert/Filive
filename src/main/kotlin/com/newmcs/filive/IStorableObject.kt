package com.newmcs.filive

import com.newmcs.filive.atomic.misc.AtomicExceptField
import com.newmcs.filive.serialize.SerializeInspector
import com.newmcs.filive.handler.IObjectHandler
import java.lang.reflect.Field
import java.nio.channels.CompletionHandler

open class IStorableObject<E : IObject<E>> : IObject<E>(), CompletionHandler<Int, IObject<*>>
{
    @AtomicExceptField
    private var inspector : SerializeInspector<IObjectHandler<*>>? = null
    fun getInspector() : SerializeInspector<IObjectHandler<*>>? = this.inspector

    override fun onCreate() : Boolean
    {
        this.inspector = SerializeInspector(this)
        return true
    }

    override fun setEnable(handleInstance: IObjectHandler<*>?)
    {
        super.setEnable(handleInstance)
        this.inspector?.setEnable(handleInstance)
    }

    override fun changedListener(field: Field, previous: Any?, after: Any?, handleInstance: Any?)
    {
        this.inspector?.asCompleted()
    }

    override fun completed(result: Int, attachment: IObject<*>)
    {

    }

    override fun failed(exc: Throwable, attachment: IObject<*>)
    {

    }
}

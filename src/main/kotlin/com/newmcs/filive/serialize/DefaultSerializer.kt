package com.newmcs.filive.serialize

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.newmcs.filive.IObject
import com.newmcs.filive.IObjectCollector
import java.lang.reflect.Type

/**
 * DefaultSerializer is adapter for IObject type.
 * Maybe you want to need to use the method for gson directly, not using IObject serialize method.
 * You can also use the GsonBuilder if registered this adapter type.
 */
class DefaultSerializer private constructor() : SerializeAdapter<IObject<*>>(IObject::class.java)
{
    companion object
    {
        fun createWithSelection(reference: Class<out IObject<*>>) : DefaultSerializer
        {
            val ds = DefaultSerializer()
            ds.setReference(reference)
            return ds
        }

        val INSTANCE : DefaultSerializer = DefaultSerializer()
    }

    override fun serialize(p0: IObject<*>, p1: Type?, p2: JsonSerializationContext?): JsonElement
    {
        return p0.getSerializeElements()
    }

    @Suppress("UNCHECKED_CAST")

    override fun deserialize(p0: JsonElement, p1: Type?, p2: JsonDeserializationContext?): IObject<*>?
    {
        return IObjectCollector.deserializeFromClass(p0, this.getReference() as Class<IObject<*>>)
    }
}
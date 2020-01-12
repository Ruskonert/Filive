package com.newmcs.filive.serialize

import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer

@Suppress("UNCHECKED_CAST")
abstract class SerializeAdapter<T>(private var reference : Class<*>) : JsonDeserializer<T>, JsonSerializer<T>
{
    fun setReference(c : Class<*>) {
        this.reference = c
    }

    fun getReference() : Class<*> {
        return this.reference
    }

    override fun hashCode(): Int {
        return reference.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SerializeAdapter<*>
        if (reference != other.reference) return false
        return true
    }
}
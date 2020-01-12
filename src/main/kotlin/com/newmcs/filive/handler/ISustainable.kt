package com.newmcs.filive.handler

interface ISustainable<K> : Handle<K>
{
    fun sustain(handleInstance : Any?)
}
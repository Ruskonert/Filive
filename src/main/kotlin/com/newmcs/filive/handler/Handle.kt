package com.newmcs.filive.handler

interface Handle<K>
{
    fun setEnable(handleInstance : K?)
    fun setEnable(enable : Boolean)
    fun isEnabled() : Boolean
}

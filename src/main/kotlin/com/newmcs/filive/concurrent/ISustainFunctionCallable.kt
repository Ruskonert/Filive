package com.newmcs.filive.concurrent

interface ISustainFunctionCallable<K, F, R>
{
    fun executeExternalSustain() : (K, F) -> R
}
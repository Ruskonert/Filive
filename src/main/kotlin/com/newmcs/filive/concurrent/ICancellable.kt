package com.newmcs.filive.concurrent

interface ICancellable
{
    fun checkIsCanceled() : Boolean

    fun executeCancel() : Boolean
}
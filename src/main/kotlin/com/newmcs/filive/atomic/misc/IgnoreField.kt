package com.newmcs.filive.atomic.misc

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class IgnoreField(val IsExpected : Boolean = false)
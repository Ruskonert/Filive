package com.newmcs.filive.atomic.misc

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AtomicExceptField(val IsExpected : Boolean = false)
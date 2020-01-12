package com.newmcs.filive.atomic.event.example

class AtomicDefaultTracker
{
    /*
     * if you register tracker method, You can just design as following:
     * @TrackerService
     * fun onChangeSomeOfType(focus : Type, target : Type) : Boolean
     * {
     *     // compare code ...
     * }
     *
     * For example, There's example class & you want to compare previous value:
     *
     * class A
     * {
     *     @Track
     *     private val a : Int = 2
     * }
     *
     * Then, You can design as following code. function name and trackerService
     * annotation are doesn't care:
     * @TrackerService
     * fun onChangeA(focus : A, target : A) : Boolean
     * {
     *     // compare code ...
     * }
     */
}

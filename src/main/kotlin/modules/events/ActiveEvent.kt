package modules.events

import java.lang.annotation.ElementType

/**
 * Is used for reflection
 * @see EventStream for it
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ActiveEvent
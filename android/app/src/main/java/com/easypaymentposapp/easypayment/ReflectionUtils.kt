package com.easypaymentposapp.easypayment

import java.lang.reflect.Method

internal object ReflectionUtils {
  private val basePackages =
      listOf(
          "br.com.paxbr.easypaymentpos",
          "br.com.paxbr.easypaymentpos.shared",
          "br.com.paxbr.easypaymentpos.shared.common",
          "br.com.paxbr.easypayment",
          "br.com.weqi.easypaymentpos",
          "br.com.weqi.easypaymentpos.shared",
          "br.com.weqi.easypaymentpos.shared.common",
          "br.com.weqi.easypayment",
      )

  private val extraPackageSuffixes =
      listOf(
          "",
          ".model",
          ".models",
          ".domain",
          ".domain.model",
          ".entity",
          ".entities",
          ".enum",
          ".enums",
          ".controller",
          ".controllers",
          ".factory",
          ".factories",
          ".callback",
          ".callbacks",
          ".interface",
          ".interfaces",
      )

  fun resolveClass(simpleName: String): Class<*> {
    val tried = mutableListOf<String>()
    for (base in basePackages) {
      for (suffix in extraPackageSuffixes) {
        val candidate = "$base$suffix.$simpleName"
        tried.add(candidate)
        runCatching { return Class.forName(candidate) }
      }
    }
    throw ClassNotFoundException("Class $simpleName not found. Tried: ${tried.joinToString()}")
  }

  fun tryResolveClass(simpleName: String): Class<*>? = runCatching { resolveClass(simpleName) }.getOrNull()

  fun method(target: Any, name: String, vararg args: Any?): Any? {
    val methods = target.javaClass.methods.filter { it.name == name && it.parameterTypes.size == args.size }
    val found = methods.firstOrNull { isCompatible(it, args) }
    if (found != null) {
      return found.invoke(target, *adaptArgs(found, args))
    }
    throw NoSuchMethodException("${target.javaClass.name}#$name/${args.size}")
  }

  fun staticMethod(clazz: Class<*>, name: String, vararg args: Any?): Any? {
    val methods = clazz.methods.filter { it.name == name && it.parameterTypes.size == args.size }
    val found = methods.firstOrNull { isCompatible(it, args) }
    if (found != null) {
      return found.invoke(null, *adaptArgs(found, args))
    }
    throw NoSuchMethodException("${clazz.name}#$name/${args.size}")
  }

  fun getProperty(instance: Any, propertyName: String): Any? {
    val getter =
        listOf(
                "get${propertyName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                "is${propertyName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
            )
            .firstNotNullOfOrNull { getterName ->
              instance.javaClass.methods.firstOrNull { it.name == getterName && it.parameterTypes.isEmpty() }
            }
    if (getter != null) {
      return getter.invoke(instance)
    }
    return null
  }

  fun setProperty(instance: Any, propertyName: String, value: Any?) {
    val setterName =
        "set${propertyName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
    val method =
        instance.javaClass.methods.firstOrNull {
          it.name == setterName && it.parameterTypes.size == 1
        }
    if (method != null) {
      method.invoke(instance, value)
      return
    }
    throw NoSuchMethodException("${instance.javaClass.name}#$setterName")
  }

  private fun isCompatible(method: Method, args: Array<out Any?>): Boolean {
    val types = method.parameterTypes
    return types.indices.all { index ->
      val arg = args[index]
      arg == null || wrap(types[index]).isAssignableFrom(arg.javaClass)
    }
  }

  private fun adaptArgs(method: Method, args: Array<out Any?>): Array<Any?> {
    val types = method.parameterTypes
    return args.mapIndexed { index, arg ->
          if (arg == null) return@mapIndexed null
          val target = wrap(types[index])
          when {
            target == java.lang.Long::class.java && arg is Number -> arg.toLong()
            target == java.lang.Integer::class.java && arg is Number -> arg.toInt()
            target == java.lang.Boolean::class.java && arg is Boolean -> arg
            target == java.lang.Double::class.java && arg is Number -> arg.toDouble()
            target == java.lang.Float::class.java && arg is Number -> arg.toFloat()
            else -> arg
          }
        }
        .toTypedArray()
  }

  private fun wrap(clazz: Class<*>): Class<*> =
      when (clazz) {
        java.lang.Long.TYPE -> java.lang.Long::class.java
        java.lang.Integer.TYPE -> java.lang.Integer::class.java
        java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
        java.lang.Double.TYPE -> java.lang.Double::class.java
        java.lang.Float.TYPE -> java.lang.Float::class.java
        java.lang.Short.TYPE -> java.lang.Short::class.java
        java.lang.Byte.TYPE -> java.lang.Byte::class.java
        java.lang.Character.TYPE -> java.lang.Character::class.java
        else -> clazz
      }
}

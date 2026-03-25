package com.easypaymentposapp.easypayment

import android.content.Context
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.lang.reflect.Proxy
import java.util.Locale

internal class EasyPaymentSdkBridge(private val reactContext: ReactApplicationContext) {
  private var controllersFactory: Any? = null
  private var transactionPos: Any? = null
  private var cancellationPos: Any? = null
  private var receiptPos: Any? = null
  private var splitPos: Any? = null
  private var lastTransactionResult: Any? = null
  private var lastInteractionPayload: Any? = null
  private var lastInteractionType: String? = null

  fun initializeSdk(identification: String, environment: String): String {
    val config = getPosConfig()
    val user = instantiateUser(identification, environment)
    val callback = statusCallback("initializeSdk")
    ReflectionUtils.method(config, "initialize", activityOrAppContext(), user, callback)
    return "initialize chamado"
  }

  fun registerInteractionCallback(): String {
    val config = getPosConfig()
    val callback = interactionCallback()
    runCatching {
          ReflectionUtils.method(config, "register", callback)
          true
        }
        .recoverCatching {
          ReflectionUtils.method(config, "register", activityOrAppContext())
          true
        }
        .getOrThrow()
    return "register chamado"
  }

  fun startInitialization(): String {
    val factory = getControllersFactory()
    val initializationPos = ReflectionUtils.method(factory, "createInitializationPOS", appContext()) ?: throw IllegalStateException("InitializationPOSInterface nulo")
    ReflectionUtils.method(initializationPos, "start", statusCallback("initialization"))
    return "initialization.start chamado"
  }

  fun startTransaction(
      amount: Double?,
      contactlessFlow: Boolean,
      allowSplit: Boolean,
      language: String,
      country: String,
  ): String {
    val info = instantiateTransactionInformation(language, country, allowSplit)
    if (amount != null) {
      val cents = (amount * 100.0).toLong()
      runCatching { ReflectionUtils.method(info, "setAmount", cents) }
      runCatching { ReflectionUtils.method(info, "setAmount", amount.toLong()) }
    }
    runCatching { ReflectionUtils.method(info, "setContactLessFlow", contactlessFlow) }

    val factory = getControllersFactory()
    val tx = ReflectionUtils.method(factory, "createTransactionPOS", appContext(), info)
    transactionPos = tx
    ReflectionUtils.method(tx ?: throw IllegalStateException("TransactionPOS nulo"), "start", statusCallback("transaction"))
    return "transaction.start chamado"
  }

  fun abortTransactionWithUserCancel(): String {
    val responseEnum = enumValue("ResponseEnum", "USER_CANCEL")
    ReflectionUtils.method(getPosConfig(), "response", responseEnum)
    return "response(USER_CANCEL) enviado"
  }

  fun responseUserCancel(): String = abortTransactionWithUserCancel()

  fun startCancellation(tid: String): String {
    val transactionResult = instantiateTransactionResult(tid)
    val factory = getControllersFactory()
    val cancellation = ReflectionUtils.method(factory, "createCancellationPOS", appContext(), transactionResult)
    cancellationPos = cancellation
    ReflectionUtils.method(cancellation ?: throw IllegalStateException("CancellationPOS nulo"), "start", statusCallback("cancellation"))
    return "cancellation.start chamado"
  }

  fun printReceipt(receiptType: String, index: Int): String {
    val receiptInterface = ensureReceiptPos()
    val receiptEnum = enumValue("ReceiptEnum", receiptType)
    val loadString = ReflectionUtils.tryResolveClass("LoadStringKt")
    val receipts: Any? =
        if (loadString != null) {
          runCatching { ReflectionUtils.staticMethod(loadString, "loadString", appContext(), receiptEnum.toString()) }.getOrNull()
        } else {
          null
        }

    if (receipts is List<*>) {
      val receipt = receipts.getOrNull(index) ?: throw IllegalArgumentException("Indice de comprovante invalido: $index")
      ReflectionUtils.method(receiptInterface, "print", receipt, statusCallback("printReceipt"))
      return "receipt.print chamado"
    }
    throw IllegalStateException("Nao foi possivel obter lista de comprovantes (loadString).")
  }

  fun reprintMerchantCancellation(): String = reprint("reprintMerchantCancellation")

  fun reprintMerchantTransaction(): String = reprint("reprintMerchantTransaction")

  fun reprintUserCancellation(): String = reprint("reprintUserCancellation")

  fun reprintUserTransaction(): String = reprint("reprintUserTransaction")

  fun sendReceiptSms(phone: String, transactionType: String, reprint: Boolean): String {
    val receiptInterface = ensureReceiptPos()
    val txTypeEnum = enumValue("TransactionTypeEnum", transactionType)
    val infoClass = ReflectionUtils.resolveClass("ReceiptInformation")
    val info = infoClass.constructors.first { it.parameterTypes.size == 3 }.newInstance(phone, txTypeEnum, reprint)
    val txResult = lastTransactionResult ?: throw IllegalStateException("Nenhum TransactionResult disponivel para envio de SMS.")
    ReflectionUtils.method(receiptInterface, "send", txResult, info, statusCallback("sendReceiptSms"))
    return "receipt.send chamado"
  }

  fun startSplit(nsu: String): String {
    val splitDataInfoClass = ReflectionUtils.resolveClass("SplitDataInfo")
    val splitInfo = splitDataInfoClass.getDeclaredConstructor().newInstance()
    runCatching { ReflectionUtils.method(splitInfo, "setNsu", nsu) }
    val factory = getControllersFactory()
    val split = ReflectionUtils.method(factory, "createSplitPOS", appContext(), splitInfo)
    splitPos = split
    ReflectionUtils.method(split ?: throw IllegalStateException("SplitPOS nulo"), "start", statusCallback("split"))
    return "split.start chamado"
  }

  fun responseLong(value: Double): String {
    ReflectionUtils.method(getPosConfig(), "response", value.toLong())
    return "response(Long) enviado"
  }

  fun responseString(value: String): String {
    ReflectionUtils.method(getPosConfig(), "response", value)
    return "response(String) enviado"
  }

  fun responseOk(): String {
    val responseEnum = enumValue("ResponseEnum", "OK")
    ReflectionUtils.method(getPosConfig(), "response", responseEnum)
    return "response(OK) enviado"
  }

  fun select(index: Double): String {
    ReflectionUtils.method(getPosConfig(), "select", index.toInt())
    return "select($index) enviado"
  }

  fun responseExpirationDate(month: String, year: String): String {
    val cardClass = ReflectionUtils.resolveClass("Card")
    val expirationClass =
        cardClass.declaredClasses.firstOrNull { it.simpleName == "ExpirationDate" }
            ?: ReflectionUtils.resolveClass("ExpirationDate")
    val exp = expirationClass.constructors.first { it.parameterTypes.size == 2 }.newInstance(month, year)
    ReflectionUtils.method(getPosConfig(), "response", exp)
    return "response(ExpirationDate) enviado"
  }

  fun responseCvv(cvv: String): String {
    val cardContent = ReflectionUtils.resolveClass("CardContent")
    val cvvClass = cardContent.declaredClasses.firstOrNull { it.simpleName == "Cvv" } ?: ReflectionUtils.resolveClass("Cvv")
    val obj = cvvClass.constructors.firstOrNull { it.parameterTypes.size == 1 }?.newInstance(cvv) ?: cvvClass.getDeclaredConstructor().newInstance()
    ReflectionUtils.method(getPosConfig(), "response", obj)
    return "response(CVV) enviado"
  }

  fun responseCvvStatus(status: String): String {
    val cardContent = ReflectionUtils.resolveClass("CardContent")
    val cvvClass =
        cardContent.declaredClasses.firstOrNull { it.simpleName == "Cvv" }
            ?: ReflectionUtils.resolveClass("Cvv")
    val cvvObj = cvvClass.getDeclaredConstructor().newInstance()
    // CvvStatus fica em br.com.paxbr.easypaymentpos.shared.common
    val cvvStatusClass =
        runCatching { ReflectionUtils.resolveClass("CvvStatus") }
            .recoverCatching { Class.forName("br.com.paxbr.easypaymentpos.shared.common.CvvStatus") }
            .recoverCatching { Class.forName("br.com.weqi.easypaymentpos.shared.common.CvvStatus") }
            .getOrElse { throw it }
    val cvvStatus = cvvStatusClass.enumConstants.firstOrNull { (it as Enum<*>).name == status }
        ?: throw IllegalArgumentException("CvvStatus $status nao encontrado")
    ReflectionUtils.setProperty(cvvObj, "cvvStatus", cvvStatus)
    ReflectionUtils.method(getPosConfig(), "response", cvvObj)
    return "response(CVV status=$status) enviado"
  }

  fun responseProductByIndex(index: Double): String {
    val any = requireLastInteractionAny()
    val product =
        if (any is List<*>) {
          any.getOrNull(index.toInt())
        } else {
          null
        }
            ?: throw IllegalStateException("Nenhuma lista de produtos encontrada no ultimo POSObject.")
    ReflectionUtils.method(getPosConfig(), "response", product)
    return "response(Product[$index]) enviado"
  }

  fun responseInterestInstallmentByIndex(index: Double): String {
    val any = requireLastInteractionAny()
    val container = any ?: throw IllegalStateException("Container de juros nao encontrado.")
    val installments = ReflectionUtils.getProperty(container, "interestInstallments")
    val value =
        if (installments is List<*>) installments.getOrNull(index.toInt()) else null
            ?: throw IllegalStateException("Parcela com juros nao encontrada no indice $index.")
    ReflectionUtils.method(getPosConfig(), "response", value)
    return "response(InterestInstallment[$index]) enviado"
  }

  fun getLastInteractionType(): String = lastInteractionType ?: "NONE"

  private fun reprint(method: String): String {
    val receiptInterface = ensureReceiptPos()
    ReflectionUtils.method(receiptInterface, method, statusCallback(method))
    return "$method chamado"
  }

  private fun ensureReceiptPos(): Any {
    if (receiptPos != null) return receiptPos as Any
    val factory = getControllersFactory()
    receiptPos = ReflectionUtils.method(factory, "createReceiptPOS", appContext())
    return receiptPos ?: throw IllegalStateException("ReceiptPOS nulo")
  }

  private fun instantiateUser(identification: String, environment: String): Any {
    val userClass = ReflectionUtils.resolveClass("User")
    val constructor = userClass.constructors.firstOrNull { it.parameterTypes.size == 2 } ?: throw IllegalStateException("Construtor User(String, String) nao encontrado")
    return constructor.newInstance(identification, environment)
  }

  private fun instantiateTransactionInformation(language: String, country: String, allowSplit: Boolean): Any {
    val infoClass = ReflectionUtils.resolveClass("TransactionInformation")
    val locale = Locale(language, country)
    val constructors = infoClass.constructors
    val preferred = constructors.firstOrNull { it.parameterTypes.size == 2 }
    if (preferred != null) {
      return preferred.newInstance(locale, allowSplit)
    }
    val single = constructors.firstOrNull { it.parameterTypes.size == 1 } ?: throw IllegalStateException("Construtor TransactionInformation nao encontrado")
    return single.newInstance(locale)
  }

  private fun instantiateTransactionResult(tid: String): Any {
    val txResultClass = ReflectionUtils.resolveClass("TransactionResult")
    val constructor = txResultClass.constructors.firstOrNull { it.parameterTypes.size == 1 } ?: throw IllegalStateException("Construtor TransactionResult(String) nao encontrado")
    return constructor.newInstance(tid)
  }

  private fun getControllersFactory(): Any {
    if (controllersFactory != null) return controllersFactory as Any
    val clazz = ReflectionUtils.resolveClass("ControllersFactory")
    controllersFactory =
        runCatching { ReflectionUtils.staticMethod(clazz, "getInstance") }
            .recoverCatching { clazz.getDeclaredConstructor().newInstance() }
            .recoverCatching {
              val field = clazz.getDeclaredField("INSTANCE")
              field.isAccessible = true
              field.get(null)
            }
            .getOrElse { throw IllegalStateException("ControllersFactory indisponivel", it) }
    return controllersFactory as Any
  }

  private fun getPosConfig(): Any {
    val clazz = ReflectionUtils.resolveClass("POSConfig")
    val direct = runCatching { ReflectionUtils.staticMethod(clazz, "getInstance") }.getOrNull()
    if (direct != null) return direct
    val companionField = runCatching { clazz.getDeclaredField("Companion") }.getOrNull()
    val companion = runCatching {
          companionField?.let {
            it.isAccessible = true
            it.get(null)
          }
        }
        .getOrNull()
    val viaCompanion =
        companion?.let { runCatching { ReflectionUtils.method(it, "getInstance") }.getOrNull() }
    return viaCompanion ?: throw IllegalStateException("POSConfig.getInstance retornou nulo")
  }

  private fun enumValue(enumSimpleClassName: String, enumName: String): Any {
    val enumClass = ReflectionUtils.resolveClass(enumSimpleClassName)
    val constants = enumClass.enumConstants ?: throw IllegalStateException("$enumSimpleClassName nao e enum")
    return constants.firstOrNull { (it as Enum<*>).name == enumName } ?: throw IllegalArgumentException("$enumName nao encontrado em $enumSimpleClassName")
  }

  private fun statusCallback(operation: String): Any {
    val callbackInterface = ReflectionUtils.resolveClass("CallBackUser")
    val loader = callbackInterface.classLoader
    return Proxy.newProxyInstance(loader, arrayOf(callbackInterface)) { _, method, args ->
      if (method.name == "onRequest") {
        val status = args?.firstOrNull()?.toString() ?: "UNKNOWN"
        if (operation == "transaction") {
          runCatching {
            val tx = transactionPos ?: return@runCatching
            lastTransactionResult = ReflectionUtils.getProperty(tx, "transactionResult")
          }
        }
        emitEvent("EasyPaymentPosStatus", operation, status, args?.firstOrNull())
      }
      null
    }
  }

  private fun interactionCallback(): Any {
    val callbackInterface = ReflectionUtils.resolveClass("CallBackUser")
    val loader = callbackInterface.classLoader
    return Proxy.newProxyInstance(loader, arrayOf(callbackInterface)) { _, method, args ->
      if (method.name == "onRequest") {
        val payload = args?.firstOrNull()
        val posInteraction = payload?.let { ReflectionUtils.getProperty(it, "posInteraction") }?.toString() ?: "UNKNOWN"
        lastInteractionPayload = payload
        lastInteractionType = posInteraction
        emitEvent("EasyPaymentPosInteraction", posInteraction, payload?.toString() ?: "", payload)
      }
      null
    }
  }

  private fun requireLastInteractionAny(): Any? {
    val payload = lastInteractionPayload ?: throw IllegalStateException("Nenhum POSObject recebido ainda.")
    return ReflectionUtils.getProperty(payload, "any")
        ?: throw IllegalStateException("Campo any nao encontrado no ultimo POSObject.")
  }

  private fun emitEvent(name: String, type: String, message: String, raw: Any?) {
    val map = Arguments.createMap()
    map.putString("type", type)
    map.putString("message", message)
    map.putString("raw", raw?.toString())
    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit(name, map)
  }

  private fun appContext(): Context = reactContext.applicationContext

  private fun activityOrAppContext(): Any = reactContext.currentActivity ?: appContext()
}

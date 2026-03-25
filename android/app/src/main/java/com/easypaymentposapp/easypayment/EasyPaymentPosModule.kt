package com.easypaymentposapp.easypayment

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReactContextBaseJavaModule

class EasyPaymentPosModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  private val bridge = EasyPaymentSdkBridge(reactContext)

  override fun getName(): String = "EasyPaymentPos"

  @ReactMethod
  fun initializeSdk(identification: String, environment: String, promise: Promise) =
      wrap(promise) { bridge.initializeSdk(identification, environment) }

  @ReactMethod fun registerInteractionCallback(promise: Promise) = wrap(promise) { bridge.registerInteractionCallback() }

  @ReactMethod fun startInitialization(promise: Promise) = wrap(promise) { bridge.startInitialization() }

  @ReactMethod
  fun startTransaction(
      amount: Double?,
      contactlessFlow: Boolean,
      allowSplit: Boolean,
      language: String,
      country: String,
      promise: Promise,
  ) = wrap(promise) { bridge.startTransaction(amount, contactlessFlow, allowSplit, language, country) }

  @ReactMethod
  fun abortTransactionWithUserCancel(promise: Promise) = wrap(promise) { bridge.abortTransactionWithUserCancel() }

  @ReactMethod fun responseUserCancel(promise: Promise) = wrap(promise) { bridge.responseUserCancel() }

  @ReactMethod fun startCancellation(tid: String, promise: Promise) = wrap(promise) { bridge.startCancellation(tid) }

  @ReactMethod fun printReceipt(receiptType: String, index: Double, promise: Promise) = wrap(promise) { bridge.printReceipt(receiptType, index.toInt()) }

  @ReactMethod fun reprintMerchantCancellation(promise: Promise) = wrap(promise) { bridge.reprintMerchantCancellation() }

  @ReactMethod fun reprintMerchantTransaction(promise: Promise) = wrap(promise) { bridge.reprintMerchantTransaction() }

  @ReactMethod fun reprintUserCancellation(promise: Promise) = wrap(promise) { bridge.reprintUserCancellation() }

  @ReactMethod fun reprintUserTransaction(promise: Promise) = wrap(promise) { bridge.reprintUserTransaction() }

  @ReactMethod
  fun sendReceiptSms(phone: String, transactionType: String, reprint: Boolean, promise: Promise) =
      wrap(promise) { bridge.sendReceiptSms(phone, transactionType, reprint) }

  @ReactMethod fun startSplit(nsu: String, promise: Promise) = wrap(promise) { bridge.startSplit(nsu) }

  @ReactMethod fun responseLong(value: Double, promise: Promise) = wrap(promise) { bridge.responseLong(value) }

  @ReactMethod fun responseString(value: String, promise: Promise) = wrap(promise) { bridge.responseString(value) }

  @ReactMethod fun responseOk(promise: Promise) = wrap(promise) { bridge.responseOk() }

  @ReactMethod fun select(index: Double, promise: Promise) = wrap(promise) { bridge.select(index) }

  @ReactMethod
  fun responseExpirationDate(month: String, year: String, promise: Promise) = wrap(promise) { bridge.responseExpirationDate(month, year) }

  @ReactMethod fun responseCvv(cvv: String, promise: Promise) = wrap(promise) { bridge.responseCvv(cvv) }

  @ReactMethod
  fun responseCvvStatus(status: String, promise: Promise) = wrap(promise) { bridge.responseCvvStatus(status) }

  @ReactMethod
  fun responseProductByIndex(index: Double, promise: Promise) = wrap(promise) { bridge.responseProductByIndex(index) }

  @ReactMethod
  fun responseInterestInstallmentByIndex(index: Double, promise: Promise) =
      wrap(promise) { bridge.responseInterestInstallmentByIndex(index) }

  @ReactMethod fun getLastInteractionType(promise: Promise) = wrap(promise) { bridge.getLastInteractionType() }

  private inline fun wrap(promise: Promise, block: () -> String) {
    runCatching { block() }
        .onSuccess { promise.resolve(it) }
        .onFailure { promise.reject("EASY_PAYMENT_POS_ERROR", it.message, it) }
  }
}

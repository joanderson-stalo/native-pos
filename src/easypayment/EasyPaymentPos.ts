import {NativeEventEmitter, NativeModules, Platform} from 'react-native';

type EasyPaymentNativeModule = {
  initializeSdk(identification: string, environment: string): Promise<string>;
  registerInteractionCallback(): Promise<string>;
  startInitialization(): Promise<string>;
  startTransaction(
    amount: number | null,
    contactlessFlow: boolean,
    allowSplit: boolean,
    language: string,
    country: string,
  ): Promise<string>;
  abortTransactionWithUserCancel(): Promise<string>;
  responseUserCancel(): Promise<string>;
  startCancellation(tid: string): Promise<string>;
  printReceipt(receiptType: 'TRANSACTION' | 'CANCELLATION', index: number): Promise<string>;
  reprintMerchantCancellation(): Promise<string>;
  reprintMerchantTransaction(): Promise<string>;
  reprintUserCancellation(): Promise<string>;
  reprintUserTransaction(): Promise<string>;
  sendReceiptSms(
    phone: string,
    transactionType: 'TRANSACTION' | 'CANCELLATION',
    reprint: boolean,
  ): Promise<string>;
  startSplit(nsu: string): Promise<string>;
  responseLong(value: number): Promise<string>;
  responseString(value: string): Promise<string>;
  responseOk(): Promise<string>;
  select(index: number): Promise<string>;
  responseExpirationDate(month: string, year: string): Promise<string>;
  responseCvv(cvv: string): Promise<string>;
  responseCvvStatus(status: 'EXISTS' | 'NO_EXISTS' | 'UNREADABLE'): Promise<string>;
  responseProductByIndex(index: number): Promise<string>;
  responseInterestInstallmentByIndex(index: number): Promise<string>;
  getLastInteractionType(): Promise<string>;
};

export type EasyPaymentEvent = {
  type: string;
  message: string;
  raw?: string;
};

const nativeModule: EasyPaymentNativeModule | undefined = NativeModules.EasyPaymentPos;

const nativeEmitter =
  Platform.OS === 'android' && nativeModule
    ? new NativeEventEmitter(NativeModules.EasyPaymentPos)
    : null;

function requireAndroidModule(): EasyPaymentNativeModule {
  if (Platform.OS !== 'android') {
    throw new Error('EasyPaymentPos somente disponivel no Android.');
  }
  if (!nativeModule) {
    throw new Error('Modulo nativo EasyPaymentPos nao encontrado.');
  }
  return nativeModule;
}

export const EasyPaymentPos = {
  initializeSdk: (identification: string, environment: string) =>
    requireAndroidModule().initializeSdk(identification, environment),
  registerInteractionCallback: () => requireAndroidModule().registerInteractionCallback(),
  startInitialization: () => requireAndroidModule().startInitialization(),
  startTransaction: (
    amount: number | null,
    contactlessFlow = true,
    allowSplit = false,
    language = 'pt',
    country = 'BR',
  ) =>
    requireAndroidModule().startTransaction(
      amount,
      contactlessFlow,
      allowSplit,
      language,
      country,
    ),
  abortTransactionWithUserCancel: () =>
    requireAndroidModule().abortTransactionWithUserCancel(),
  responseUserCancel: () => requireAndroidModule().responseUserCancel(),
  startCancellation: (tid: string) => requireAndroidModule().startCancellation(tid),
  printReceipt: (receiptType: 'TRANSACTION' | 'CANCELLATION', index = 1) =>
    requireAndroidModule().printReceipt(receiptType, index),
  reprintMerchantCancellation: () =>
    requireAndroidModule().reprintMerchantCancellation(),
  reprintMerchantTransaction: () =>
    requireAndroidModule().reprintMerchantTransaction(),
  reprintUserCancellation: () => requireAndroidModule().reprintUserCancellation(),
  reprintUserTransaction: () => requireAndroidModule().reprintUserTransaction(),
  sendReceiptSms: (
    phone: string,
    transactionType: 'TRANSACTION' | 'CANCELLATION',
    reprint = false,
  ) => requireAndroidModule().sendReceiptSms(phone, transactionType, reprint),
  startSplit: (nsu: string) => requireAndroidModule().startSplit(nsu),
  responseLong: (value: number) => requireAndroidModule().responseLong(value),
  responseString: (value: string) => requireAndroidModule().responseString(value),
  responseOk: () => requireAndroidModule().responseOk(),
  select: (index: number) => requireAndroidModule().select(index),
  responseExpirationDate: (month: string, year: string) =>
    requireAndroidModule().responseExpirationDate(month, year),
  responseCvv: (cvv: string) => requireAndroidModule().responseCvv(cvv),
  responseCvvStatus: (status: 'EXISTS' | 'NO_EXISTS' | 'UNREADABLE') =>
    requireAndroidModule().responseCvvStatus(status),
  responseProductByIndex: (index: number) =>
    requireAndroidModule().responseProductByIndex(index),
  responseInterestInstallmentByIndex: (index: number) =>
    requireAndroidModule().responseInterestInstallmentByIndex(index),
  getLastInteractionType: () => requireAndroidModule().getLastInteractionType(),
  onStatus: (listener: (event: EasyPaymentEvent) => void) =>
    nativeEmitter?.addListener('EasyPaymentPosStatus', listener),
  onInteraction: (listener: (event: EasyPaymentEvent) => void) =>
    nativeEmitter?.addListener('EasyPaymentPosInteraction', listener),
};


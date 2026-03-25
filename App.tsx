import React, {useEffect, useState} from 'react';
import {Alert, Button, SafeAreaView, ScrollView, StatusBar, StyleSheet, Text, TextInput} from 'react-native';
import {EasyPaymentEvent, EasyPaymentPos} from './src/easypayment/EasyPaymentPos';

function App(): React.JSX.Element {
  const [identification, setIdentification] = useState('21995127035');
  const [environment, setEnvironment] = useState('https://52.168.167.13');
  const [amount, setAmount] = useState('1.00');
  const [log, setLog] = useState<string[]>([]);

  useEffect(() => {
    const statusSub = EasyPaymentPos.onStatus((event: EasyPaymentEvent) => {
      appendLog(`STATUS ${event.type}: ${event.message}`);
    });
    const interactionSub = EasyPaymentPos.onInteraction((event: EasyPaymentEvent) => {
      appendLog(`INTERACTION ${event.type}: ${event.message}`);
    });
    return () => {
      statusSub?.remove();
      interactionSub?.remove();
    };
  }, []);

  async function run(label: string, action: () => Promise<string>) {
    try {
      const result = await action();
      appendLog(`${label}: ${result}`);
    } catch (error) {
      const message =
        error instanceof Error ? error.message : 'Erro inesperado na chamada';
      appendLog(`${label} erro: ${message}`);
      Alert.alert('EasyPaymentPos', message);
    }
  }

  function appendLog(line: string) {
    console.log(line);
    setLog(prev => [new Date().toISOString() + ' - ' + line, ...prev].slice(0, 60));
  }

  async function quickPay() {
    const steps: Array<[string, () => Promise<string>]> = [
      ['initializeSdk', () => EasyPaymentPos.initializeSdk(identification, environment)],
      ['registerInteractionCallback', () => EasyPaymentPos.registerInteractionCallback()],
      ['startInitialization', () => EasyPaymentPos.startInitialization()],
      [
        'startTransaction',
        () => EasyPaymentPos.startTransaction(Number(amount), true, false, 'pt', 'BR'),
      ],
    ];
    for (const [label, action] of steps) {
      try {
        const result = await action();
        appendLog(`${label}: ${result}`);
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Erro inesperado na chamada';
        appendLog(`${label} erro: ${message}`);
        Alert.alert('EasyPaymentPos', `${label}: ${message}`);
        break;
      }
    }
  }

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>EasyPayment POS - SDK Calls</Text>


        <Text style={styles.label}>Identification (CPF/CNPJ)</Text>
        <TextInput
          style={styles.input}
          value={identification}
          onChangeText={setIdentification}
        />

        <Text style={styles.label}>Environment URL</Text>
        <TextInput
          style={styles.input}
          value={environment}
          onChangeText={setEnvironment}
        />

        <Text style={styles.label}>Amount</Text>
        <TextInput style={styles.input} value={amount} onChangeText={setAmount} />
        <Button title="Pagar agora" onPress={quickPay} />

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, backgroundColor: '#F6F7F8'},
  content: {padding: 16, gap: 12},
  title: {fontSize: 20, fontWeight: '700', marginBottom: 4},
  label: {fontSize: 13, fontWeight: '600'},
  input: {
    borderWidth: 1,
    borderColor: '#D0D5DD',
    borderRadius: 8,
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
});

export default App;

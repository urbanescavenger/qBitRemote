import React, { useState, useContext } from 'react';
import { StyleSheet, Image, TextInput, KeyboardAvoidingView, Button, Scrollview  } from 'react-native';
import AppContext from '../global/AppContext'
import EditScreenInfo from '../components/EditScreenInfo';
import { qbLogin } from '../global/qbApi';
import { Text, View } from '../components/Themed';
import * as SecureStore from 'expo-secure-store';


async function save(key, value) {
      await SecureStore.setItemAsync(key, value);
}


async function getValueFor(key) {
  let result = await SecureStore.getItemAsync(key);
  if (result) {
    alert("🔐 Here's your value 🔐 \n" + result);
  } else {
    alert('No values stored under that key.');
  }
}


export default function TabTwoScreen({navigation}) {

  const userSettings:any = useContext(AppContext);

  const [key, onChangeKey] = React.useState('');
  const [value, onChangeValue] = React.useState('');
  const [host, setHost] = React.useState(userSettings.host);
  const [port, setPort] = React.useState(userSettings.port);
  const [ssl, setSsl] = React.useState();
  const [username, setUsername] = React.useState(userSettings.username);
  const [password, setPassword] = React.useState(userSettings.password);
  const [test, setTest] = React.useState('');



const testLogin = async () => {
  const result = await qbLogin({ host, port, ssl: userSettings.ssl, username, password });
  if (result.ok) {
save('host', host);
save('port', port);
save('username', username);
save('passwordRes', password);
userSettings.setHost(host);
userSettings.setPort(port);
userSettings.setUsername(username);
userSettings.setPassword(password);
alert('Settings saved')
    } else {
      let reason: string;
      if (result.error) reason = `network: ${result.error}`;
      else if (result.status === 401) reason = 'HTTP 401 — wrong username or password';
      else if (result.status === 403) reason = 'HTTP 403 — IP banned by qBittorrent (restart it / wait / whitelist this IP)';
      else reason = `HTTP ${result.status ?? '?'}: ${result.body ?? ''}`;
      alert(`Could not auth with server.\n\n${reason}`)
    }
}
  return (
    <View darkColor="black" style={styles.container}>
      <Image style={{width: 100, height: 100}} source={require('../assets/images/icon2.png')} />
<View style={{height: 20}} />
      <Text style={styles.title}>qBitRemote</Text>
      
      <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

<Button
        title="Host Settings"
        onPress={() => {
        navigation.navigate('HostScreen')
        }}
      />


      <EditScreenInfo path="/screens/TabTwoScreen.tsx" />



    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',

    },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
  },
 input: {
    height: 40,
    margin: 12,
    borderWidth: 1,
    padding: 10,
  },
  separator: {
    marginVertical: 30,
    height: 1,
    width: '80%',
  },
});

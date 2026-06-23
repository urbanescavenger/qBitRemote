import React, { useState, useContext } from 'react';
import { StyleSheet, Image, TextInput, KeyboardAvoidingView, Button, ScrollView, Switch  } from 'react-native';
import AppContext from '../global/AppContext'


import EditScreenInfo from '../components/EditScreenInfo';
import { Text, View } from '../components/Themed';
import * as SecureStore from 'expo-secure-store';
import { qbLogin } from '../global/qbApi';


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


export default function HostScreen() {

  const userSettings:any = useContext(AppContext);

  const [key, onChangeKey] = React.useState('');
  const [value, onChangeValue] = React.useState('');
  const [host, setHost] = React.useState(userSettings.host);
  const [nickname, setNickname] = React.useState(userSettings.nickname ?? '');
  const [port, setPort] = React.useState(userSettings.port);
  const [ssl, setSsl] = React.useState();
  const [username, setUsername] = React.useState(userSettings.username);
  const [password, setPassword] = React.useState(userSettings.password);
  const [test, setTest] = React.useState('');
  const [isSwitchOn, setIsSwitchOn] = React.useState(userSettings.ssl == 'true' ? true : false);
  const onToggleSwitch = () => {
    setIsSwitchOn(!isSwitchOn)

  }


const testLogin = async () => {

save('ssl', isSwitchOn.toString());
userSettings.setSsl(isSwitchOn.toString());

const result = await qbLogin({ host, port, ssl: isSwitchOn.toString(), username, password });
if (result.ok) {
save('host', host);
save('port', port);
save('username', username);
save('passwordRes', password);
save('ssl', isSwitchOn.toString());
save('nickname', nickname);


userSettings.setHost(host);
userSettings.setPort(port);
userSettings.setUsername(username);
userSettings.setPassword(password);
userSettings.setSsl(isSwitchOn.toString());
userSettings.setNickname(nickname);

alert('设置已保存')
} else {
  // Surface why it failed so we can tell bad creds / IP ban / network error apart.
  let reason: string;
  if (result.error) reason = `network: ${result.error}`;
  else if (result.status === 401) reason = 'HTTP 401 — 用户名或密码错误';
  else if (result.status === 403) reason = 'HTTP 403 — 被 qBittorrent 封禁 IP(重启它 / 等待 / 加白名单)';
  else reason = `HTTP ${result.status ?? '?'}: ${result.body ?? ''}`;
  alert(`无法通过服务器验证。\n\n${reason}`)
}

}
  
  return (
    <ScrollView style={styles.container}>
      <Text style={styles.info}>名称</Text>
  
      <View  darkColor="#1c1c1c" style={styles.cards}>
      <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
  <View darkColor="rgba(255,255,255,0)" style={styles.leftContainer}>
          
          <Text style={styles.getStartedText}>昵称</Text>
        </View>

  <View darkColor="rgba(255,255,255,0)" style={styles.rightContainer}>

                   <TextInput

      placeholder={"服务器名?"}

        style={styles.input}
        onChangeText={setNickname}
        value={nickname}
      />
        </View>

        </View>
</View>


      <Text style={styles.info}>qBittorrent 账号</Text>

      <View darkColor="#1c1c1c" style={styles.cards}>



<View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
  <View darkColor="rgba(255,255,255,0)" style={styles.leftContainer}>

          <Text style={styles.getStartedText}>地址</Text>
        </View>

  <View darkColor="rgba(255,255,255,0)" style={styles.rightContainer}>

           <TextInput

      placeholder={"qBittorrent 地址"}

        style={styles.input}
        onChangeText={setHost}
        value={host}
      />
        </View>
       
        </View>
      <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />


<View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
  <View darkColor="rgba(255,255,255,0)" style={styles.leftContainer}>

          <Text style={styles.getStartedText}>端口</Text>
        </View>

  <View darkColor="rgba(255,255,255,0)" style={styles.rightContainer}>

      <TextInput
      placeholder={"WebUI 端口"}
        style={styles.input}
        onChangeText={setPort}
        value={port}
      />
        </View>
       
        </View>

      <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

<View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>启用 SSL</Text>
         <Switch value={isSwitchOn} onValueChange={onToggleSwitch} />
        </View>
      <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

<View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
  <View darkColor="rgba(255,255,255,0)" style={styles.leftContainer}>

          <Text style={styles.getStartedText}>用户名</Text>
          </View>
  <View  darkColor="rgba(255,255,255,0)" style={styles.rightContainer}>

      <TextInput
      placeholder={"输入用户名"}

        style={styles.input}
        onChangeText={setUsername}
        value={username}
      />
        </View>
       
        </View>

      <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />


<View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
  <View darkColor="rgba(255,255,255,0)" style={styles.leftContainer}>

          <Text style={styles.getStartedText}>密码</Text>
          </View>

           <View darkColor="rgba(255,255,255,0)" style={styles.rightContainer}>

      <TextInput
      placeholder={"输入密码"}

        style={styles.input}
        onChangeText={setPassword}
        value={password}
        secureTextEntry={true}
      />
        </View>
       
        </View>
</View>




<Text style={{textAlign: 'center'}}>请求格式为 "SSL+地址+':'+端口"</Text>
<Text style={{textAlign: 'center'}}>端口后直接接相对路径,末尾不要加 '/'</Text>


<Button
        title="保存"
        onPress={() => {
        testLogin();
        }}
      />



    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    },
      getStartedText: {
    fontSize: 17,
    lineHeight: 24,
  },
    getStartedText: {
    fontSize: 17,
    lineHeight: 24,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
  },
 input: {
 width: '100%',
 textAlign: 'right'
  },
   leftContainer: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'flex-start',

  },
   rightContainer: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'flex-end',
    alignItems: 'center',

  },
  separator: {
    marginVertical: 10,
    height: 1,
    width: '100%',

  },
   data: {
    color: 'grey',
    fontSize: 17,
    lineHeight: 24,
    textAlign: 'right',
    flex: 1,
    flexWrap: 'wrap'
  },
  cards: {
    margin: 20,
    marginTop: 5,
    padding: 10,
    borderRadius: 15,

  },
    info: {
    fontSize: 13,
    lineHeight: 24,
    marginLeft: 30,
    fontWeight: '400'
  }
});

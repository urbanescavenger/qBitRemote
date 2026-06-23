import React from 'react';
import { StyleSheet, Image, Button } from 'react-native';
import EditScreenInfo from '../components/EditScreenInfo';
import { Text, View } from '../components/Themed';

export default function TabTwoScreen({ navigation }) {
  return (
    <View darkColor="black" style={styles.container}>
      <Image style={{ width: 100, height: 100 }} source={require('../assets/images/icon2.png')} />
      <View style={{ height: 20 }} />
      <Text style={styles.title}>qBitRemote</Text>

      <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

      <Button
        title="服务器设置"
        onPress={() => navigation.navigate('HostScreen')}
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
  separator: {
    marginVertical: 30,
    height: 1,
    width: '80%',
  },
});

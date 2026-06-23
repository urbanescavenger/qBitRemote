import React, { useState, useEffect, useContext } from 'react';
import AppContext from '../global/AppContext'

import { Ionicons } from '@expo/vector-icons';

import { StyleSheet, FlatList, TouchableOpacity, ColorSchemeName, TouchableNativeFeedback, Button, Text as RNText, View as RNView } from 'react-native';

import EditScreenInfo from '../components/EditScreenInfo';
import { Text, View } from '../components/Themed';
import { ProgressBar } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { qbLogin, qbGet } from '../global/qbApi';



export default function TabOneScreen({ navigation, colorScheme }: { navigation: any, colorScheme: ColorSchemeName }) {
  const [torrents, setTorrents] = useState([]);
  const [clinetInfo, setClientInfo] = useState([]);

  const userSettings: any = useContext(AppContext);


  const loginQbit = async () => {
    await qbLogin(userSettings);
    getTorrentsQbit();
    getTorrentsQbitInfo();
  }


  const getTorrentsQbitInfo = async () => {
    const result = await qbGet(userSettings, '/api/v2/transfer/info');
    if (result) {
      setClientInfo(result);
    }
  }

  const getTorrentsQbit = async () => {
    getTorrentsQbitInfo();

    const result = await qbGet(userSettings, '/api/v2/torrents/info?sort=added_on&reverse=true');
    if (result) {
      setTorrents(result);
      console.log('Recicved');
      setRefreshed(false);
    }
  }


  React.useEffect(() => {

    loginQbit();

    const timer = setInterval(() => getTorrentsQbit(), 3000)

    const timerInfo = setInterval(() => getTorrentsQbitInfo(), 3000)


    const unsubscribe = navigation.addListener('focus', () => {

      getTorrentsQbit()
      getTorrentsQbitInfo()
    });

    return () => {
      clearInterval(timer);
      clearInterval(timerInfo);
      unsubscribe();
    };
  }, [navigation]);




  const onPress = (click: any) => console.log(click) + navigation.navigate('InfoScreen', { data: click });
  const onPressLong = (clickL: any) => setRefreshed(false);


  const [refreshed, setRefreshed] = useState(false);

  const onRefresh = () => {
    setRefreshed(true);
    getTorrentsQbit();

  }

  function formatBytes(bytes, decimals = 2) {
    if (bytes === 0) return '0 B';
    if (bytes === NaN) return '0 B';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  }

  const _handleMore = () => navigation.navigate('UploadScreen');


  return (

    <View style={styles.container}>


      <SafeAreaView edges={['top']} style={{ backgroundColor: '#2f6fed' }}>
        <RNView style={{ height: 56, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16 }}>
          <RNView style={{ flex: 1 }}>
            <RNText style={{ color: '#fff', fontSize: 18, fontWeight: '700' }}>远程</RNText>
            <RNText style={{ color: '#dbeafe', fontSize: 12, marginTop: 2 }}>
              ↑{clinetInfo.up_info_speed == null ? '0' : formatBytes(clinetInfo.up_info_speed)}/s  ↓{clinetInfo.dl_info_speed == null ? '0' : formatBytes(clinetInfo.dl_info_speed)}/s
            </RNText>
          </RNView>
          <TouchableOpacity onPress={_handleMore} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }} style={{ paddingHorizontal: 12 }}>
            <RNText style={{ color: '#fff', fontSize: 30, fontWeight: '700', lineHeight: 34 }}>+</RNText>
          </TouchableOpacity>
        </RNView>
      </SafeAreaView>


      <FlatList
        data={torrents}
        style={{ flex: 1 }}
        onRefresh={() => onRefresh()}
        refreshing={refreshed}
        renderItem={({ item }) => (

          <TouchableOpacity style={styles.row} onPress={() => onPress(item)} onLongPress={() => onPressLong(item.name)}>
            <Text style={{ textAlign: 'center', marginBottom: 5 }} numberOfLines={1}>{item.name}</Text>


            <ProgressBar style={{ height: 3, width: '100%', borderRadius: 20 }} progress={item.progress} color="#42A5F5" />


            <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>

              {(() => {
                if (item.state == 'stalledUP') {
                  return (
                    <Text style={styles.markdown}>做种</Text>
                  )
                }
                if (item.state == 'pausedDL') {
                  return (
                    <Text style={styles.markdown}>已暂停</Text>
                  )
                }
                if (item.state == 'uploading') {
                  return (
                    <Text style={styles.markdown}>做种</Text>
                  )
                }

                return <Text style={styles.markdown}>{item.state}</Text>;
              })()}

              <Text style={styles.markdown}>↑ {formatBytes(item.uploaded)} ↓ {

                formatBytes(item.downloaded)}</Text>
              <Text style={styles.markdown}>↑ {formatBytes(item.upspeed, 1)}/s ↓ {formatBytes(item.dlspeed, 1)}/s</Text>
              <Text style={styles.markdown}>{Math.round(item.ratio * 100) / 100}</Text>
            </View>

            <View style={{ height: 1, width: '100%', backgroundColor: '#D1D1D6' }}></View>
          </TouchableOpacity>


        )}
        keyExtractor={({ hash }, index) => hash}
      />


    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  row: {
    flex: 1,
    marginTop: 20,
    justifyContent: 'center',
    marginLeft: 25,
    marginRight: 25,
  },
  markdown: {
    textAlign: 'center',
    fontSize: 13,
    marginTop: 7,
    marginBottom: 7,
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

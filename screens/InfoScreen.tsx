import React, { useState, useEffect, useContext } from 'react';
import AppContext from '../global/AppContext'

import { ScrollView, StyleSheet, TouchableOpacity, Button, Vibration, Alert } from 'react-native';

import EditScreenInfo from '../components/EditScreenInfo';
import { Text, View } from '../components/Themed';
import { qbAction } from '../global/qbApi';

const reason = (r: any) =>
  r.error ? `network: ${r.error}`
  : r.status === 401 ? 'HTTP 401 — 未认证'
  : r.status === 403 ? 'HTTP 403 — 被拒绝(可能 IP 被封)'
  : `HTTP ${r.status ?? '?'}: ${r.body ?? ''}`;

export default function InfoScreen({ route, navigation }) {
  const { data } = route.params;
  const userSettings: any = useContext(AppContext);


  const deleteTorrent = () => {
    Alert.alert(
      "删除种子?",
      data.name,
      [
        {
          text: "取消",
          onPress: () => console.log("Cancel Pressed"),
          style: "cancel"
        },
        {
          text: "删除", onPress: async () => {
            const r = await qbAction(userSettings, "/api/v2/torrents/delete?hashes=" + data.hash + "&deleteFiles=true");
            if (r.ok) {
              Vibration.vibrate();
              navigation.goBack();
            } else {
              alert(`无法删除种子。\n\n${reason(r)}`);
            }
          }
        }
      ]
    );
  }
  const pauseTorrent = async () => {
    const r = await qbAction(userSettings, "/api/v2/torrents/pause?hashes=" + data.hash);
    if (r.ok) { Vibration.vibrate(); } else { alert(`无法暂停种子。\n\n${reason(r)}`); }
  }
  const recheckTorrent = async () => {
    const r = await qbAction(userSettings, "/api/v2/torrents/recheck?hashes=" + data.hash);
    if (r.ok) { Vibration.vibrate(); } else { alert(`无法重新校验种子。\n\n${reason(r)}`); }
  }
  const resumeTorrent = async () => {
    const r = await qbAction(userSettings, "/api/v2/torrents/resume?hashes=" + data.hash);
    if (r.ok) { Vibration.vibrate(); } else { alert(`无法恢复种子。\n\n${reason(r)}`); }
  }

  return (

    <ScrollView>
      <View style={{ marginTop: 25 }} />

      <Text style={styles.info}>操作</Text>
      <View darkColor="#1c1c1c" style={styles.cards}>
        <Button
          title="暂停"
          onPress={() => pauseTorrent()}
        />


        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

        <Button
          title="恢复"
          onPress={() => resumeTorrent()}
        />

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

        <Button
          title="重新校验"
          onPress={() => recheckTorrent()}
        />


        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />
        <Button
          color="red"
          title="删除种子"
          onPress={() => deleteTorrent()}
        />

      </View>

      <Text style={styles.info}>基本信息</Text>
      <View darkColor="#1c1c1c" style={styles.cards}>
        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>名称</Text>
          <Text style={styles.data} >{data.name} </Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>状态</Text>
          <Text style={styles.data} >{data.state} </Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />


        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>大小</Text>
          <Text style={styles.data} >{Math.round(data.total_size / (1024 * 1024 * 1024) * 100) / 100} GB </Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />
        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>种子数</Text>
          <Text style={styles.data} >{data.num_complete}</Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />
        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>分类</Text>
          <Text style={styles.data} >{data.category}</Text>
        </View>

      </View>



      <Text style={styles.info}>下载信息</Text>
      <View darkColor="#1c1c1c" style={styles.cards}>
        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>已下载</Text>
          <Text style={styles.data} >{Math.round(data.downloaded / (1024 * 1024 * 1024) * 100) / 100} GB </Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>已上传</Text>
          <Text style={styles.data} >{Math.round(data.uploaded / (1024 * 1024 * 1024) * 100) / 100} GB  </Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />


        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>比率</Text>
          <Text style={styles.data} >{Math.round(data.ratio * 100) / 100}</Text>
        </View>
        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />


        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>做种时间</Text>
          <Text style={styles.data} >{new Date(data.seeding_time * 1000).toISOString().substr(11, 8)}</Text>
        </View>
      </View>


      <Text style={styles.info}>存储</Text>
      <View darkColor="#1c1c1c" style={styles.cards}>
        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>路径</Text>
          <Text style={styles.data} >{data.save_path}</Text>
        </View>



      </View>






      <Text style={styles.info}>Tracker</Text>
      <View darkColor="#1c1c1c" style={styles.cards}>
        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>Tracker</Text>
          <Text style={styles.data} >{data.tracker} </Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />



        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>数量</Text>
          <Text style={styles.data} >{data.trackers_count}</Text>
        </View>

      </View>



      <View style={{ margin: 30 }} />
    </ScrollView>

  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  button: {
    marginRight: 20,
    marginLeft: 20,
    marginTop: 10,
    paddingTop: 10,
    paddingBottom: 10,
    backgroundColor: '#1E6738',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#fff'
  },
  loginText: {
    color: '#fff',
    textAlign: 'center',
    fontSize: 16,
    paddingLeft: 10,
    paddingRight: 10
  },
  cards: {
    margin: 20,
    marginTop: 5,
    padding: 10,
    borderRadius: 15,

  },
  getStartedText: {
    fontSize: 17,
    lineHeight: 24,
  },
  info: {
    fontSize: 13,
    lineHeight: 24,
    marginLeft: 30,
    fontWeight: '400'
  },
  data: {
    color: 'grey',
    fontSize: 17,
    lineHeight: 24,
    textAlign: 'right',
    flex: 1,
    flexWrap: 'wrap'
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
  },
  separator: {
    marginVertical: 10,
    height: 1,
    width: '100%',
  },
});

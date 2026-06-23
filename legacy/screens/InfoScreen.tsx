import React, { useState, useEffect, useContext } from 'react';
import AppContext from '../global/AppContext'

import { ScrollView, StyleSheet, TouchableOpacity, Button, Vibration, Alert } from 'react-native';

import EditScreenInfo from '../components/EditScreenInfo';
import { Text, View } from '../components/Themed';
import { qbAction, qbGet } from '../global/qbApi';

const reason = (r: any) =>
  r.error ? `network: ${r.error}`
  : r.status === 401 ? 'HTTP 401 — 未认证'
  : r.status === 403 ? 'HTTP 403 — 被拒绝(可能 IP 被封)'
  : `HTTP ${r.status ?? '?'}: ${r.body ?? ''}`;

function formatBytes(bytes: any, decimals = 2) {
  if (!bytes) return '0 B';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

export default function InfoScreen({ route, navigation }) {
  const { data } = route.params;
  const userSettings: any = useContext(AppContext);

  // Live snapshot of this torrent, polled every few seconds so the speed
  // readout stays current. Initialized from the list snapshot in route params.
  const [torrent, setTorrent] = useState(data);

  const refreshTorrent = async () => {
    const result = await qbGet(userSettings, `/api/v2/torrents/info?hashes=${encodeURIComponent(torrent.hash)}`);
    if (Array.isArray(result) && result.length > 0) {
      setTorrent(result[0]);
    }
  };

  useEffect(() => {
    refreshTorrent();
    const timer = setInterval(refreshTorrent, 3000);
    const unsubscribe = navigation.addListener('focus', refreshTorrent);
    return () => {
      clearInterval(timer);
      unsubscribe();
    };
  }, [navigation]);


  const deleteTorrent = () => {
    Alert.alert(
      "删除种子?",
      torrent.name,
      [
        {
          text: "取消",
          onPress: () => console.log("Cancel Pressed"),
          style: "cancel"
        },
        {
          text: "删除", onPress: async () => {
            const r = await qbAction(userSettings, "/api/v2/torrents/delete", { hashes: torrent.hash, deleteFiles: "true" });
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
    const r = await qbAction(userSettings, "/api/v2/torrents/stop", { hashes: torrent.hash });
    if (r.ok) { Vibration.vibrate(); } else { alert(`无法暂停种子。\n\n${reason(r)}`); }
  }
  const recheckTorrent = async () => {
    const r = await qbAction(userSettings, "/api/v2/torrents/recheck", { hashes: torrent.hash });
    if (r.ok) { Vibration.vibrate(); } else { alert(`无法重新校验种子。\n\n${reason(r)}`); }
  }
  const resumeTorrent = async () => {
    const r = await qbAction(userSettings, "/api/v2/torrents/start", { hashes: torrent.hash });
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
          <Text style={styles.data} >{torrent.name} </Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>状态</Text>
          <Text style={styles.data} >{torrent.state} </Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />


        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>大小</Text>
          <Text style={styles.data} >{Math.round(torrent.total_size / (1024 * 1024 * 1024) * 100) / 100} GB </Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />
        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>种子数</Text>
          <Text style={styles.data} >{torrent.num_complete}</Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />
        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>分类</Text>
          <Text style={styles.data} >{torrent.category}</Text>
        </View>

      </View>



      <Text style={styles.info}>当前速度</Text>
      <View darkColor="#1c1c1c" style={styles.cards}>
        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>上传</Text>
          <Text style={styles.data} >{formatBytes(torrent.upspeed, 1)}/s </Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>下载</Text>
          <Text style={styles.data} >{formatBytes(torrent.dlspeed, 1)}/s </Text>
        </View>
      </View>



      <Text style={styles.info}>下载信息</Text>
      <View darkColor="#1c1c1c" style={styles.cards}>
        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>已下载</Text>
          <Text style={styles.data} >{Math.round(torrent.downloaded / (1024 * 1024 * 1024) * 100) / 100} GB </Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>已上传</Text>
          <Text style={styles.data} >{Math.round(torrent.uploaded / (1024 * 1024 * 1024) * 100) / 100} GB  </Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />


        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>比率</Text>
          <Text style={styles.data} >{Math.round(torrent.ratio * 100) / 100}</Text>
        </View>
        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />


        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>做种时间</Text>
          <Text style={styles.data} >{new Date(torrent.seeding_time * 1000).toISOString().substr(11, 8)}</Text>
        </View>
      </View>


      <Text style={styles.info}>存储</Text>
      <View darkColor="#1c1c1c" style={styles.cards}>
        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>路径</Text>
          <Text style={styles.data} >{torrent.save_path}</Text>
        </View>



      </View>






      <Text style={styles.info}>Tracker</Text>
      <View darkColor="#1c1c1c" style={styles.cards}>
        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>Tracker</Text>
          <Text style={styles.data} >{torrent.tracker} </Text>
        </View>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />



        <View darkColor="rgba(255,255,255,0)" style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
          <Text style={styles.getStartedText}>数量</Text>
          <Text style={styles.data} >{torrent.trackers_count}</Text>
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

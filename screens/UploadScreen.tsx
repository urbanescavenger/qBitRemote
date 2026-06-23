import React, { useState, useContext } from 'react'
import AppContext from '../global/AppContext'

import { Text, View } from '../components/Themed';
import EditScreenInfo from '../components/EditScreenInfo';

import { StyleSheet, TouchableOpacity, TextInput, Button, ScrollView, Modal, FlatList } from 'react-native';
import * as DocumentPicker from 'expo-document-picker';
import * as Clipboard from 'expo-clipboard';
import { Ionicons } from '@expo/vector-icons';
import { qbLogin, qbGet, qbBaseUrl } from '../global/qbApi';

import { RootStackParamList } from '../types';

export default function UploadScreen({
  navigation,
}) {
  const userSettings:any = useContext(AppContext);

  const [selectedCat, setSelectedCat] = useState("uncategorized");
  const [allCat, setAllCat] = useState<Record<string, any>>({});
  const [docPicked, setDocPicked] = useState<DocumentPicker.DocumentPickerAsset | null>(null);

  const [magnet, setMagnet] = React.useState("");
  const [catPickerOpen, setCatPickerOpen] = React.useState(false);

  // POST a magnet link / URL string to qBittorrent. Shared by the manual input
  // box and the "add from clipboard" button; respects the selected category.
  const addByUrl = async (urls: string) => {
    const trimmed = (urls ?? "").trim();
    if (!trimmed) {
      alert("未提供磁链或 URL");
      return;
    }
    await qbLogin(userSettings);

    var formdata = new FormData();
    formdata.append("urls", trimmed);
    if(selectedCat != "uncategorized") {
      formdata.append("category", selectedCat);
  }

    try {
      const res = await fetch(qbBaseUrl(userSettings) + "/api/v2/torrents/add", {
        method: 'POST',
        credentials: 'include',
        body: formdata,
      });
      const body = await res.text();
      check(res.status, body);
    } catch (error) {
      console.log('error', error);
      alert(`无法添加种子。\n\nnetwork: ${error?.message ?? String(error)}`)
    }
  }

  const addFromClipboard = async () => {
    const texts = await Clipboard.getStringAsync();
    addByUrl(texts);
  }
  const check = (status: number, body: string) => {
    // Newer qBittorrent returns 204 No Content on success; older returns 200 "Ok."
    const ok = status === 204
      || (status === 200 && body.trim().toLowerCase() === 'ok.');
    if (ok) {
      navigation.goBack()
    } else {
      alert(`无法添加种子。\n\nHTTP ${status}: ${body || '(no body)'}`)
    }
  }

const getCategory = async () => {
    const result = await qbGet(userSettings, '/api/v2/sync/maindata');
    if (result) {
      setAllCat(result.categories ?? {});
    }
}

const sendTorrent = async () => {

  if (!docPicked) {
    return;
  }

  await qbLogin(userSettings);

  var data = new FormData();
  data.append("torrents", {
    uri: docPicked.uri,
    name: docPicked.name ?? "torrent",
    type: docPicked.mimeType ?? 'application/x-bittorrent',
  } as any, docPicked.name ?? "torrent");
  if(selectedCat != "uncategorized") {
      data.append("category", selectedCat);
  }



  var xhr = new XMLHttpRequest();
  xhr.withCredentials = true;

  xhr.addEventListener("readystatechange", function () {
    if (this.readyState === 4) {
      console.log(this.responseText);
      check(this.status, this.responseText);
    }
  });

  xhr.open("POST", qbBaseUrl(userSettings) + "/api/v2/torrents/add");

  xhr.send(data);

}

  const _pickDocument = async () => {
    let result = await DocumentPicker.getDocumentAsync({});
    if (result.canceled || !result.assets || result.assets.length === 0) {
      return;
    }
    setDocPicked(result.assets[0]);
  }

  React.useEffect(() => {
  getCategory();
  }, [navigation]);
  return (
    <ScrollView style={styles.container}>
   
      <Text style={styles.info}>选择文件</Text>
      <View darkColor="#1c1c1c" style={styles.cards}>
        <Button
          title='从剪贴板添加'
          onPress={() => addFromClipboard()}
        />


        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

        <Button
          title='选择种子文件'
          onPress={() => _pickDocument()}
        />

      </View>


 

      <Text style={styles.info}>磁链 / URL</Text>
      <View darkColor="#1c1c1c" style={styles.cards}>
        <TextInput
          placeholder={"粘贴磁链或 URL"}
          style={styles.input}
          onChangeText={setMagnet}
          value={magnet}
          autoCapitalize="none"
          autoCorrect={false}
        />

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

        <Button
          title='添加'
          onPress={() => addByUrl(magnet)}
        />
      </View>

      <Text style={styles.info}>选择分类 </Text>
      <View darkColor="#1c1c1c" style={styles.cards}>

        <TouchableOpacity
          style={styles.catRow}
          onPress={() => setCatPickerOpen(true)}
        >
          <Text style={styles.catLabel}>
            {selectedCat === 'uncategorized' ? '未分类' : selectedCat}
          </Text>
          <Ionicons name="chevron-down" size={18} color="#888" />
        </TouchableOpacity>

        <Modal
          visible={catPickerOpen}
          transparent
          animationType="fade"
          onRequestClose={() => setCatPickerOpen(false)}
        >
          <TouchableOpacity
            style={styles.modalOverlay}
            activeOpacity={1}
            onPress={() => setCatPickerOpen(false)}
          >
            <View style={styles.modalSheet} lightColor="#fff" darkColor="#2a2a2a">
              <Text style={styles.modalTitle}>选择分类</Text>
              <FlatList
                data={[
                  { name: 'uncategorized', label: '未分类' },
                  ...Object.keys(allCat ?? {}).map((key) => ({
                    name: allCat[key].name,
                    label: allCat[key].name,
                  })),
                ]}
                keyExtractor={(item) => item.name}
                renderItem={({ item }) => {
                  const active = item.name === selectedCat;
                  return (
                    <TouchableOpacity
                      style={[styles.catOption, active && styles.catOptionActive]}
                      onPress={() => {
                        setSelectedCat(item.name);
                        setCatPickerOpen(false);
                      }}
                    >
                      <Text style={[styles.catOptionText, active && styles.catOptionTextActive]}>
                        {item.label}
                      </Text>
                      {active && <Ionicons name="checkmark" size={18} color="#2f6fed" />}
                    </TouchableOpacity>
                  );
                }}
              />
            </View>
          </TouchableOpacity>
        </Modal>

        <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />

        <Button
          title='发送'
          onPress={() => sendTorrent()}
        />


      </View>

  








    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
  },
  upload: {

    height: '100%',
    width: '100%',
    backgroundColor: 'rgba(248, 249, 254, 0.1)',
    borderWidth: 2,
    borderStyle: 'dashed',
    borderColor: 'grey',
    borderRadius: 1
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
  },
  input: {
    height: 40,
    width: '100%',
    margin: 12,
    borderWidth: 1,
    padding: 10,
  },
  link: {
    marginTop: 15,
    paddingVertical: 15,
  },
  linkText: {
    fontSize: 14,
    color: '#2e78b7',
  },
  smallText: {
    fontSize: 14,
    marginTop: 5
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
  separator: {
    marginVertical: 10,
    height: 1,
    width: '100%',
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
    marginTop: 15,
    padding: 10,
    borderRadius: 15,

  },
  catRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    height: 44,
    paddingHorizontal: 8,
  },
  catLabel: {
    fontSize: 16,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center',
    padding: 24,
  },
  modalSheet: {
    borderRadius: 12,
    padding: 12,
    maxHeight: 360,
  },
  modalTitle: {
    fontSize: 15,
    fontWeight: '600',
    paddingVertical: 8,
    paddingHorizontal: 8,
  },
  catOption: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 12,
    paddingHorizontal: 8,
    borderRadius: 8,
  },
  catOptionActive: {
    backgroundColor: 'rgba(47,111,237,0.12)',
  },
  catOptionText: {
    fontSize: 15,
  },
  catOptionTextActive: {
    color: '#2f6fed',
    fontWeight: '600',
  },
  getStartedText: {
    fontSize: 17,
    lineHeight: 24,
  },
});

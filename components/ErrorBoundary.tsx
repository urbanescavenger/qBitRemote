import React from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet } from 'react-native';

// Catches render errors anywhere below it so a broken screen shows a readable
// message instead of a silent pure-white screen (which is what a release build
// shows by default when an uncaught error unmounts the tree). In dev it shows
// the full error + stack; in release a short message + a retry hint.
type Props = { children: React.ReactNode };
type State = { error: Error | null };

export default class ErrorBoundary extends React.Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: { componentStack?: string | null }) {
    // Surface in the JS console / Metro logs so the cause is findable.
    console.log('ErrorBoundary caught:', error, info?.componentStack);
  }

  reset = () => this.setState({ error: null });

  render() {
    const { error } = this.state;
    if (!error) return this.props.children;

    const isDev = __DEV__;
    return (
      <View style={styles.container}>
        <Text style={styles.title}>界面出错了</Text>
        <Text style={styles.message}>{error.message || String(error)}</Text>
        {isDev && (
          <ScrollView style={styles.scroll} contentContainerStyle={styles.scrollContent}>
            <Text style={styles.stack}>{error.stack}</Text>
          </ScrollView>
        )}
        <TouchableOpacity style={styles.button} onPress={this.reset}>
          <Text style={styles.buttonText}>重试</Text>
        </TouchableOpacity>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 24,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    marginBottom: 12,
    color: '#d33',
  },
  message: {
    fontSize: 14,
    color: '#333',
    textAlign: 'center',
    marginBottom: 16,
  },
  scroll: {
    width: '100%',
    maxHeight: 300,
    marginBottom: 16,
  },
  scrollContent: {
    alignItems: 'flex-start',
  },
  stack: {
    fontSize: 11,
    color: '#888',
    fontFamily: 'monospace',
  },
  button: {
    paddingVertical: 10,
    paddingHorizontal: 24,
    backgroundColor: '#2f6fed',
    borderRadius: 8,
  },
  buttonText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
  },
});

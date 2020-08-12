import { NativeModules } from 'react-native';
const { UsageStats } = NativeModules;
export default {
  async getStats(day, excludes = ['system', 'google']) {
    await this.checkPermission();
    const res = await UsageStats.getStats(day, excludes);
    return JSON.parse(res);
  },
  async getAppStats(packageName, day = 365) {
    await this.checkPermission();
    const res = await UsageStats.getAppStats(packageName, day);
    return JSON.parse(res);
  },
  async checkPermission() {
    const granted = await UsageStats.checkPermission();
    if (granted) {
      return true;
    } else {
      throw new Error(() => UsageStats.requestPermission());
    }
  },
  requestPermission() {
    return UsageStats.requestPermission();
  },
}

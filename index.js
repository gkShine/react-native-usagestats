import { NativeModules } from 'react-native';
export default {
  getModule() {
    return NativeModules.UsageStats;
  },
  async getStats(day = 365, excludes = ['system', 'google']) {
    await this.checkPermission();
    const res = await this.getModule().getStats(day, excludes);
    return JSON.parse(res);
  },
  async getAppStats(packageName, day = 365) {
    await this.checkPermission();
    const res = await this.getModule().getAppStats(packageName, day);
    return JSON.parse(res);
  },
  async checkPermission() {
    const granted = await this.getModule().checkPermission();
    if (granted) {
      return true;
    } else {
      throw new Error(() => this.getModule().requestPermission());
    }
  },
  requestPermission() {
    return this.getModule().requestPermission();
  },
}

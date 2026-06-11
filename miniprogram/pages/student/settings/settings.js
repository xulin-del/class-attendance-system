Page({
  data: {
    signResult: true,
    showStats: true
  },

  onLoad() {
    this.loadSettings()
  },

  loadSettings() {
    const settings = wx.getStorageSync('studentSettings') || {}
    this.setData({
      signResult: settings.signResult !== false,
      showStats: settings.showStats !== false
    })
  },

  saveSettings() {
    const { signResult, showStats } = this.data
    wx.setStorageSync('studentSettings', {
      signResult,
      showStats
    })
  },

  toggleSignResult(e) {
    this.setData({ signResult: e.detail.value })
    this.saveSettings()
  },

  toggleShowStats(e) {
    this.setData({ showStats: e.detail.value })
    this.saveSettings()
  },

  openLocationSetting() {
    wx.openSetting({
      success: (res) => {
        if (res.authSetting['scope.userLocation']) {
          wx.showToast({ title: '定位权限已开启', icon: 'success' })
        }
      }
    })
  },

  clearCache() {
    wx.showModal({
      title: '提示',
      content: '确定要清除缓存吗？',
      success: (res) => {
        if (res.confirm) {
          try {
            const userInfo = wx.getStorageSync('userInfo')
            const userType = wx.getStorageSync('userType')
            wx.clearStorageSync()
            wx.setStorageSync('userInfo', userInfo)
            wx.setStorageSync('userType', userType)
            wx.showToast({ title: '清除成功', icon: 'success' })
          } catch (e) {
            wx.showToast({ title: '清除失败', icon: 'none' })
          }
        }
      }
    })
  },

  checkUpdate() {
    wx.showToast({ title: '已是最新版本', icon: 'success' })
  }
})
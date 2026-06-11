Page({
  data: {
    signReminder: true,
    absentReminder: true,
    showAvatar: true,
    expireIndex: 2,
    expireOptions: [
      { value: 10, label: '10分钟' },
      { value: 20, label: '20分钟' },
      { value: 30, label: '30分钟' },
      { value: 60, label: '60分钟' }
    ],
    radiusIndex: 2,
    radiusOptions: [
      { value: 50, label: '50米' },
      { value: 100, label: '100米' },
      { value: 200, label: '200米' },
      { value: 500, label: '500米' }
    ]
  },

  onLoad() {
    this.loadSettings()
  },

  loadSettings() {
    const settings = wx.getStorageSync('teacherSettings') || {}
    this.setData({
      signReminder: settings.signReminder !== false,
      absentReminder: settings.absentReminder !== false,
      showAvatar: settings.showAvatar !== false,
      expireIndex: settings.expireIndex || 2,
      radiusIndex: settings.radiusIndex || 2
    })
  },

  saveSettings() {
    const { signReminder, absentReminder, showAvatar, expireIndex, radiusIndex } = this.data
    wx.setStorageSync('teacherSettings', {
      signReminder,
      absentReminder,
      showAvatar,
      expireIndex,
      radiusIndex
    })
  },

  toggleSignReminder(e) {
    this.setData({ signReminder: e.detail.value })
    this.saveSettings()
  },

  toggleAbsentReminder(e) {
    this.setData({ absentReminder: e.detail.value })
    this.saveSettings()
  },

  toggleShowAvatar(e) {
    this.setData({ showAvatar: e.detail.value })
    this.saveSettings()
  },

  changeExpireTime(e) {
    this.setData({ expireIndex: parseInt(e.detail.value) })
    this.saveSettings()
  },

  changeRadius(e) {
    this.setData({ radiusIndex: parseInt(e.detail.value) })
    this.saveSettings()
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
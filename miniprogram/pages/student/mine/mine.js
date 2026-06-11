const app = getApp()

Page({
  data: {
    userInfo: {}
  },

  onLoad() {
    this.checkLogin()
  },

  onShow() {
    this.loadUserInfo()
  },

  checkLogin() {
    const userInfo = wx.getStorageSync('userInfo')
    const userType = wx.getStorageSync('userType')
    if (!userInfo || userType !== 'student') {
      wx.redirectTo({ url: '/pages/login/login' })
    } else {
      this.setData({ userInfo })
    }
  },

  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo')
    this.setData({ userInfo })
  },

  goToAi() {
    wx.navigateTo({ url: '/pages/student/ai/ai' })
  },

  goToMakeup() {
    wx.redirectTo({ url: '/pages/student/records/records?makeup=1' })
  },

  goToProfile() {
    wx.navigateTo({ url: '/pages/student/profile/profile' })
  },

  goToHelp() {
    wx.navigateTo({ url: '/pages/student/help/help' })
  },

  goToSettings() {
    wx.navigateTo({ url: '/pages/student/settings/settings' })
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('userInfo')
          wx.removeStorageSync('userType')
          app.globalData.userInfo = null
          app.globalData.userType = ''
          wx.redirectTo({ url: '/pages/login/login' })
        }
      }
    })
  },

  switchTab(e) {
    const index = e.currentTarget.dataset.index
    const urls = [
      '/pages/student/index/index',
      '/pages/student/sign/sign',
      '/pages/student/records/records',
      '/pages/student/mine/mine'
    ]
    if (index !== 3) {
      wx.redirectTo({ url: urls[index] })
    }
  }
})

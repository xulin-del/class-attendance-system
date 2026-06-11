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
    if (!userInfo || userType !== 'teacher') {
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
    wx.navigateTo({ url: '/pages/teacher/ai/ai' })
  },

  goToProfile() {
    wx.navigateTo({ url: '/pages/teacher/profile/profile' })
  },

  goToHelp() {
    wx.navigateTo({ url: '/pages/teacher/help/help' })
  },

  goToSettings() {
    wx.navigateTo({ url: '/pages/teacher/settings/settings' })
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
      '/pages/teacher/index/index',
      '/pages/teacher/course/course',
      '/pages/teacher/sign/sign',
      '/pages/teacher/statistics/statistics',
      '/pages/teacher/mine/mine'
    ]
    if (index !== 4) {
      wx.redirectTo({ url: urls[index] })
    }
  }
})
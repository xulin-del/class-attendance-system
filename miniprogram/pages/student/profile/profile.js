Page({
  data: {
    userInfo: {}
  },

  onLoad() {
    this.loadUserInfo()
  },

  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo')
    this.setData({ userInfo })
  },

  viewAttendance() {
    wx.navigateTo({ url: '/pages/student/records/records' })
  },

  viewSubscriptions() {
    wx.navigateTo({ url: '/pages/student/subscription/subscription' })
  }
})
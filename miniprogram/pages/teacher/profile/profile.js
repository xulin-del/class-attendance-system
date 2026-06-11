const app = getApp()

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

  changePassword() {
    wx.showModal({
      title: '修改密码',
      editable: true,
      placeholderText: '请输入新密码',
      success: (res) => {
        if (res.confirm && res.content) {
          wx.showToast({
            title: '密码修改成功',
            icon: 'success'
          })
        }
      }
    })
  }
})
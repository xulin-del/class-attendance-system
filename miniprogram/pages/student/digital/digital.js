const { post } = require('../../../utils/api')

Page({
  data: {
    inputValue: [],
    submitting: false
  },

  inputDigit(e) {
    const digit = e.currentTarget.dataset.digit
    if (this.data.inputValue.length < 4) {
      this.setData({
        inputValue: [...this.data.inputValue, digit]
      })
    }
  },

  deleteDigit() {
    const arr = this.data.inputValue
    if (arr.length > 0) {
      arr.pop()
      this.setData({ inputValue: arr })
    }
  },

  async submitSignIn() {
    if (this.data.inputValue.length !== 4) {
      wx.showToast({ title: '请输入4位数字', icon: 'none' })
      return
    }

    const token = this.data.inputValue.join('')
    const userInfo = wx.getStorageSync('userInfo')

    this.setData({ submitting: true })

    try {
      const res = await post('/digital/signIn', {
        token: token,
        studentId: userInfo.studentId
      })

      if (res.success) {
        wx.showToast({ title: '签到成功', icon: 'success' })
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      } else {
        wx.showToast({ title: res.message || '签到失败', icon: 'none' })
        this.setData({ inputValue: [] })
      }
    } catch (err) {
      console.error(err)
      wx.showToast({ title: '网络错误', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  }
})

const app = getApp()
const { get, post } = require('../../../utils/api')

Page({
  data: {
    userInfo: {},
    courses: [],
    selectedCourse: null,
    signType: 'qrcode',
    currentLocation: '',
    signCode: ''
  },

  onLoad(options) {
    this.checkLogin()
    if (options.signCode) {
      this.setData({ signCode: options.signCode })
    }
  },

  onShow() {
    this.loadCourses()
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

  async loadCourses() {
    try {
      const res = await get('/course/listByClass', { className: this.data.userInfo.className })
      if (res.success) {
        this.setData({ courses: res.data })
        if (res.data.length > 0 && !this.data.selectedCourse) {
          this.setData({ selectedCourse: res.data[0] })
        }
      }
    } catch (err) {
      console.error(err)
    }
  },

  onCourseChange(e) {
    const index = e.detail.value
    this.setData({ selectedCourse: this.data.courses[index] })
  },

  selectSignType(e) {
    this.setData({ signType: e.currentTarget.dataset.type })
  },

  onSignCodeInput(e) {
    this.setData({ signCode: e.detail.value })
  },

  scanQRCode() {
    const that = this
    wx.scanCode({
      onlyFromCamera: false,
      scanType: ['qrCode', 'barCode'],
      success: async (res) => {
        var result = res.result
        var signCode = ''
        if (result) {
          if (result.startsWith('ATT_')) {
            signCode = result
          } else {
            var match = result.match(/ATT_\d+_\d+/)
            if (match) {
              signCode = match[0]
            }
          }
        }

        if (signCode) {
          that.setData({ signCode: signCode })
          await that.signInByCode(signCode)
        } else {
          wx.showModal({
            title: '提示',
            content: '未识别到有效签到码，请尝试手动输入',
            showCancel: false
          })
        }
      },
      fail(err) {
        if (err.errMsg && err.errMsg.indexOf('cancel') === -1) {
          wx.showToast({ title: '扫码失败，请重试', icon: 'none' })
        }
      }
    })
  },

  async signInByCode(signCode) {
    const { userInfo } = this.data
    if (!signCode || !signCode.trim()) {
      wx.showToast({ title: '请输入签到码', icon: 'none' })
      return
    }
    if (!userInfo || !userInfo.studentId) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }

    signCode = signCode.trim()

    wx.showLoading({ title: '签到中...' })
    try {
      const res = await post('/attendance/signInByCode', {
        signCode: signCode,
        studentId: userInfo.studentId
      })
      wx.hideLoading()

      if (res.success) {
        wx.showModal({
          title: '签到成功',
          content: '你已成功完成扫码签到',
          showCancel: false,
          confirmText: '好的'
        })
      } else {
        wx.showModal({
          title: '签到失败',
          content: res.message || '签到失败，请重试',
          showCancel: false
        })
      }
    } catch (err) {
      wx.hideLoading()
      wx.showModal({
        title: '签到失败',
        content: '网络错误，请检查网络后重试',
        showCancel: false
      })
    }
  },

  async submitSignCode() {
    const { signCode } = this.data
    if (!signCode) {
      wx.showToast({ title: '请输入签到码', icon: 'none' })
      return
    }
    await this.signInByCode(signCode)
  },

  async locationSignIn() {
    const { selectedCourse, userInfo } = this.data
    if (!selectedCourse) {
      wx.showToast({ title: '请选择课程', icon: 'none' })
      return
    }

    wx.showLoading({ title: '获取位置...' })
    try {
      const location = await this.getGPSCoordinates()
      if (!location) {
        wx.hideLoading()
        wx.showModal({
          title: '定位失败',
          content: '无法获取您的位置信息，请检查是否开启了定位权限',
          showCancel: false
        })
        return
      }

      this.setData({
        currentLocation: '纬度: ' + location.latitude.toFixed(6) + ', 经度: ' + location.longitude.toFixed(6)
      })

      wx.showLoading({ title: '签到中...' })
      const res = await post('/attendance/signInByLocation', {
        courseId: selectedCourse.courseId,
        studentId: userInfo.studentId,
        latitude: location.latitude,
        longitude: location.longitude
      })
      wx.hideLoading()

      if (res.success) {
        wx.showModal({
          title: '签到成功',
          content: '你已成功完成定位签到（距离签到点' + (res.data.distance || '') + '米）',
          showCancel: false,
          confirmText: '好的'
        })
      } else {
        wx.showModal({
          title: '签到失败',
          content: res.message || '签到失败，请重试',
          showCancel: false
        })
      }
    } catch (err) {
      wx.hideLoading()
      wx.showModal({
        title: '签到失败',
        content: '网络错误，请检查网络后重试',
        showCancel: false
      })
    }
  },

  getGPSCoordinates() {
    return new Promise((resolve) => {
      wx.getLocation({
        type: 'gcj02',
        success(res) {
          resolve({ latitude: res.latitude, longitude: res.longitude })
        },
        fail(err) {
          console.error('获取位置失败:', err)
          resolve(null)
        }
      })
    })
  },

  goToDigitalSign() {
    wx.navigateTo({ url: '/pages/student/digital/digital' })
  },

  switchTab(e) {
    const index = e.currentTarget.dataset.index
    const urls = [
      '/pages/student/index/index',
      '/pages/student/sign/sign',
      '/pages/student/records/records',
      '/pages/student/mine/mine'
    ]
    if (index !== 1) {
      wx.redirectTo({ url: urls[index] })
    }
  }
})

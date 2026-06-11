const app = getApp()
const { post } = require('../../utils/api')

Page({
  data: {
    showGuide: true,
    userType: 'teacher',
    userId: '',
    password: '',
    showRegister: false,
    showForgot: false,
    registerForm: {
      userId: '',
      name: '',
      className: '',
      password: '',
      phone: ''
    },
    forgotForm: {
      userId: '',
      phone: ''
    },
    recoveredPassword: ''
  },

  onLoad() {
    this.setNavigationTheme('#f7fbf8')
  },

  enterLogin() {
    this.setData({ showGuide: false })
    this.setNavigationTheme('#f8fbfa')
  },

  switchRole(e) {
    this.setData({
      userType: e.currentTarget.dataset.role,
      userId: '',
      password: '',
      recoveredPassword: '',
      registerForm: {
        userId: '',
        name: '',
        className: '',
        password: '',
        phone: ''
      },
      forgotForm: {
        userId: '',
        phone: ''
      }
    })
  },

  onUserIdInput(e) {
    this.setData({ userId: e.detail.value })
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },

  onRegisterInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`registerForm.${field}`]: e.detail.value })
  },

  onForgotInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`forgotForm.${field}`]: e.detail.value })
  },

  openRegister() {
    this.setData({ showRegister: true, showForgot: false, recoveredPassword: '' })
  },

  closeRegister() {
    this.setData({ showRegister: false })
  },

  openForgot() {
    this.setData({ showForgot: true, showRegister: false, recoveredPassword: '' })
  },

  closeForgot() {
    this.setData({ showForgot: false, recoveredPassword: '' })
  },

  stopTap() {},

  setNavigationTheme(backgroundColor) {
    wx.setNavigationBarColor({
      frontColor: '#000000',
      backgroundColor
    })
    wx.setBackgroundColor({
      backgroundColor
    })
  },

  async handleLogin() {
    const { userType, userId, password } = this.data

    if (!userId) {
      wx.showToast({ title: '请输入账号', icon: 'none' })
      return
    }

    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }

    wx.showLoading({ title: '登录中...' })

    try {
      const res = userType === 'teacher'
        ? await post('/teacher/login', { teacherId: userId, password })
        : await post('/student/login', { studentId: userId, password })

      wx.hideLoading()

      if (res && res.success) {
        app.globalData.userInfo = res.data
        app.globalData.userType = userType
        wx.setStorageSync('userInfo', res.data)
        wx.setStorageSync('userType', userType)

        wx.showToast({ title: '登录成功', icon: 'success' })

        setTimeout(() => {
          wx.redirectTo({
            url: userType === 'teacher' ? '/pages/teacher/index/index' : '/pages/student/index/index'
          })
        }, 700)
      } else {
        wx.showToast({ title: res?.message || '登录失败', icon: 'none' })
      }
    } catch (err) {
      console.error('login error:', err)
      wx.hideLoading()
      wx.showToast({ title: '网络错误', icon: 'none' })
    }
  },

  async handleRegister() {
    const { userType, registerForm } = this.data
    const { userId, name, className, password, phone } = registerForm

    if (!userId || !name || !password || !phone || (userType === 'student' && !className)) {
      wx.showToast({ title: '请完整填写注册信息', icon: 'none' })
      return
    }

    wx.showLoading({ title: '注册中...' })

    try {
      const res = userType === 'teacher'
        ? await post('/teacher/register', {
          teacherId: userId,
          teacherName: name,
          password,
          phone
        })
        : await post('/student/register', {
          studentId: userId,
          studentName: name,
          className,
          password,
          phone
        })

      wx.hideLoading()

      if (res && res.success) {
        wx.showToast({ title: '注册成功', icon: 'success' })
        this.setData({
          showRegister: false,
          userId,
          password
        })
      } else {
        wx.showToast({ title: res?.message || '注册失败', icon: 'none' })
      }
    } catch (err) {
      console.error('register error:', err)
      wx.hideLoading()
      wx.showToast({ title: '网络错误', icon: 'none' })
    }
  },

  async handleForgotPassword() {
    const { userType, forgotForm } = this.data
    const { userId, phone } = forgotForm

    if (!userId || !phone) {
      wx.showToast({ title: '请输入账号和手机号', icon: 'none' })
      return
    }

    wx.showLoading({ title: '验证中...' })

    try {
      const res = userType === 'teacher'
        ? await post('/teacher/forgotPassword', { teacherId: userId, phone })
        : await post('/student/forgotPassword', { studentId: userId, phone })

      wx.hideLoading()

      if (res && res.success) {
        this.setData({ recoveredPassword: res.data.password || '' })
      } else {
        this.setData({ recoveredPassword: '' })
        wx.showToast({ title: res?.message || '验证失败', icon: 'none' })
      }
    } catch (err) {
      console.error('forgot password error:', err)
      wx.hideLoading()
      wx.showToast({ title: '网络错误', icon: 'none' })
    }
  }
})

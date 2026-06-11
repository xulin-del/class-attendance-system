App({
  globalData: {
    // 环境配置切换
    // 本地开发: http://localhost:8081/api
    // 局域网: http://192.168.x.x:8081/api
    // 生产环境: https://your-domain.com/api
    baseUrl: 'http://localhost:8081/api',
    userInfo: null,
    userType: '',
    courseSignStates: {}
  },
  onLaunch() {
    const userInfo = wx.getStorageSync('userInfo');
    const userType = wx.getStorageSync('userType');
    if (userInfo && userType) {
      this.globalData.userInfo = userInfo;
      this.globalData.userType = userType;
    }
  },

  getCourseSignState(courseId) {
    if (!this.globalData.courseSignStates[courseId]) {
      this.globalData.courseSignStates[courseId] = {
        qrcode: {
          active: false,
          signCode: '',
          endTime: 0
        },
        location: {
          active: false,
          signCode: '',
          endTime: 0,
          teacherLocation: '',
          latitude: null,
          longitude: null,
          radius: 100
        }
      }
    }
    return this.globalData.courseSignStates[courseId]
  },

  setQRCodeSignState(courseId, active, signCode, endTime) {
    var state = this.getCourseSignState(courseId)
    state.qrcode.active = active
    state.qrcode.signCode = signCode
    state.qrcode.endTime = endTime
  },

  setLocationSignState(courseId, active, signCode, endTime, teacherLocation, latitude, longitude, radius) {
    var state = this.getCourseSignState(courseId)
    state.location.active = active
    state.location.signCode = signCode
    state.location.endTime = endTime
    state.location.teacherLocation = teacherLocation || ''
    state.location.latitude = latitude
    state.location.longitude = longitude
    state.location.radius = radius || 100
  },

  clearQRCodeSignState(courseId) {
    this.setQRCodeSignState(courseId, false, '', 0)
  },

  clearLocationSignState(courseId) {
    this.setLocationSignState(courseId, false, '', 0, '', null, null, 100)
  }
})

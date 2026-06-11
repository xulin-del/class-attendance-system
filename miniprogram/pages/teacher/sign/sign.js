const app = getApp()
const { get, post } = require('../../../utils/api')
const QRCode = require('../../../utils/qrcode')

Page({
  data: {
    userInfo: {},
    courses: [],
    selectedCourse: null,
    signType: 'qrcode',
    duration: 5,
    isQRCodeSigning: false,
    isLocationSigning: false,
    isDigitalSigning: false,
    qrcodeSignCode: '',
    qrcodeCountdown: 0,
    qrcodeSignedCount: 0,
    qrcodeUnsignedCount: 0,
    locationSignCode: '',
    locationCountdown: 0,
    locationSignedCount: 0,
    locationUnsignedCount: 0,
    teacherLocation: '',
    teacherLatitude: null,
    teacherLongitude: null,
    locationRadius: 100,
    digitalToken: '',
    digitalCountdown: 0,
    digitalSignedCount: 0,
    digitalUnsignedCount: 0,
    qrcodeTimer: null,
    locationTimer: null,
    digitalTimer: null,
    qrcodePollingTimer: null,
    locationPollingTimer: null,
    digitalPollingTimer: null,
    coursesLoaded: false
  },

  onLoad() {
    this.checkLogin()
  },

  onShow() {
    if (!this.data.coursesLoaded) {
      this.loadCourses()
    } else {
      this.restoreSignState()
    }
  },

  onUnload() {
    this.clearAllTimers()
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

  async loadCourses() {
    try {
      const res = await get('/course/list', { teacherId: this.data.userInfo.teacherId })
      if (res.success) {
        var selectedCourse = this.data.selectedCourse
        if (res.data.length > 0 && !selectedCourse) {
          selectedCourse = res.data[0]
        }
        
        this.setData({
          courses: res.data,
          selectedCourse: selectedCourse,
          coursesLoaded: true
        }, () => {
          this.restoreSignState()
        })
      }
    } catch (err) {
      console.error(err)
    }
  },

  restoreSignState() {
    var courseId = this.data.selectedCourse ? this.data.selectedCourse.courseId : null
    if (!courseId) return

    var state = app.getCourseSignState(courseId)
    var now = Date.now()

    this.clearAllTimers()

    if (state.qrcode.active && state.qrcode.endTime > now) {
      var remaining = Math.floor((state.qrcode.endTime - now) / 1000)
      this.setData({
        isQRCodeSigning: true,
        qrcodeSignCode: state.qrcode.signCode,
        qrcodeCountdown: remaining,
        signType: 'qrcode',
        isLocationSigning: false,
        locationSignCode: '',
        locationCountdown: 0,
        teacherLocation: '',
        teacherLatitude: null,
        teacherLongitude: null
      }, () => {
        this.generateQRCode(state.qrcode.signCode)
        this.startQRCodeCountdown()
        this.startQRCodePolling()
      })
    } else if (state.qrcode.active && state.qrcode.endTime <= now) {
      app.clearQRCodeSignState(courseId)
      this.setData({
        isQRCodeSigning: false,
        qrcodeSignCode: '',
        qrcodeCountdown: 0
      })
    } else {
      this.setData({
        isQRCodeSigning: false,
        qrcodeSignCode: '',
        qrcodeCountdown: 0
      })
    }

    if (state.location.active && state.location.endTime > now) {
      var remaining2 = Math.floor((state.location.endTime - now) / 1000)
      this.setData({
        isLocationSigning: true,
        locationSignCode: state.location.signCode,
        locationCountdown: remaining2,
        teacherLocation: state.location.teacherLocation,
        teacherLatitude: state.location.latitude,
        teacherLongitude: state.location.longitude,
        locationRadius: state.location.radius,
        signType: 'location',
        isQRCodeSigning: false,
        qrcodeSignCode: '',
        qrcodeCountdown: 0
      }, () => {
        this.startLocationCountdown()
        this.startLocationPolling()
      })
    } else if (state.location.active && state.location.endTime <= now) {
      app.clearLocationSignState(courseId)
      this.setData({
        isLocationSigning: false,
        locationSignCode: '',
        locationCountdown: 0,
        teacherLocation: '',
        teacherLatitude: null,
        teacherLongitude: null
      })
    } else {
      this.setData({
        isLocationSigning: false,
        locationSignCode: '',
        locationCountdown: 0,
        teacherLocation: '',
        teacherLatitude: null,
        teacherLongitude: null
      })
    }
  },

  onCourseChange(e) {
    const index = e.detail.value
    const newCourse = this.data.courses[index]
    
    this.clearAllTimers()
    
    this.setData({
      selectedCourse: newCourse,
      isQRCodeSigning: false,
      qrcodeSignCode: '',
      qrcodeCountdown: 0,
      qrcodeSignedCount: 0,
      qrcodeUnsignedCount: 0,
      isLocationSigning: false,
      locationSignCode: '',
      locationCountdown: 0,
      locationSignedCount: 0,
      locationUnsignedCount: 0,
      teacherLocation: '',
      teacherLatitude: null,
      teacherLongitude: null
    })
    
    this.restoreSignState()
  },

  selectSignType(e) {
    var newType = e.currentTarget.dataset.type
    var oldType = this.data.signType
    
    this.setData({ signType: newType })
    
    if (newType === 'qrcode' && oldType !== 'qrcode' && this.data.isQRCodeSigning) {
      this.generateQRCode(this.data.qrcodeSignCode)
    }
  },

  selectDuration(e) {
    this.setData({ duration: Number(e.currentTarget.dataset.duration) })
  },

  onRadiusInput(e) {
    this.setData({ locationRadius: Number(e.detail.value) || 100 })
  },

  async startSign() {
    const { selectedCourse, signType } = this.data
    if (!selectedCourse) {
      wx.showToast({ title: '请选择课程', icon: 'none' })
      return
    }

    if (signType === 'location') {
      await this.startLocationSign()
    } else {
      await this.startQRCodeSign()
    }
  },

  async startQRCodeSign() {
    const { selectedCourse, duration } = this.data
    wx.showLoading({ title: '发起签到...' })
    try {
      const res = await post('/attendance/start', {
        courseId: selectedCourse.courseId,
        className: selectedCourse.className
      })
      wx.hideLoading()

      if (res.success) {
        var endTime = Date.now() + duration * 60 * 1000
        
        app.setQRCodeSignState(
          selectedCourse.courseId,
          true,
          res.data.signCode,
          endTime
        )

        this.setData({
          isQRCodeSigning: true,
          qrcodeSignCode: res.data.signCode,
          qrcodeCountdown: duration * 60,
          qrcodeSignedCount: 0,
          qrcodeUnsignedCount: 0
        })

        this.generateQRCode(res.data.signCode)
        this.startQRCodeCountdown()
        this.startQRCodePolling()

        this.createSignReminder(res.data.signCode, selectedCourse.courseId)

        wx.showToast({ title: '签到已发起', icon: 'success' })
      } else {
        wx.showToast({ title: res.message, icon: 'none' })
      }
    } catch (err) {
      wx.hideLoading()
      wx.showToast({ title: '网络错误', icon: 'none' })
    }
  },

  async startLocationSign() {
    const { selectedCourse, locationRadius } = this.data
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

      wx.showLoading({ title: '发起定位签到...' })
      const res = await post('/attendance/startLocation', {
        courseId: selectedCourse.courseId,
        className: selectedCourse.className,
        latitude: location.latitude,
        longitude: location.longitude,
        radius: locationRadius
      })
      wx.hideLoading()

      if (res.success) {
        var endTime = Date.now() + 30 * 60 * 1000
        var teacherLocation = '纬度: ' + location.latitude.toFixed(6) + ', 经度: ' + location.longitude.toFixed(6)
        
        app.setLocationSignState(
          selectedCourse.courseId,
          true,
          res.data.signCode,
          endTime,
          teacherLocation,
          location.latitude,
          location.longitude,
          locationRadius
        )

        this.setData({
          isLocationSigning: true,
          locationSignCode: res.data.signCode,
          teacherLocation: teacherLocation,
          teacherLatitude: location.latitude,
          teacherLongitude: location.longitude,
          locationCountdown: 30 * 60,
          locationSignedCount: 0,
          locationUnsignedCount: 0
        })

        this.startLocationCountdown()
        this.startLocationPolling()

        this.createSignReminder(res.data.signCode, selectedCourse.courseId)

        wx.showToast({ title: '定位签到已发起', icon: 'success' })
      } else {
        wx.showToast({ title: res.message, icon: 'none' })
      }
    } catch (err) {
      wx.hideLoading()
      wx.showToast({ title: '网络错误', icon: 'none' })
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

  generateQRCode(signCode) {
    var that = this
    setTimeout(function () {
      that.doGenerateQRCode(signCode, 5)
    }, 300)
  },

  doGenerateQRCode(signCode, retryCount) {
    var that = this
    if (retryCount <= 0) {
      console.error('generateQRCode failed after retries')
      return
    }

    console.log('开始查询 canvas 节点, retryCount:', retryCount)

    wx.createSelectorQuery()
      .select('#qrcodeCanvas')
      .fields({ node: true, size: true })
      .exec(function (res) {
        console.log('SelectorQuery 结果:', res)

        if (!res || !res[0] || !res[0].node) {
          console.warn('canvas node not found, retrying...', retryCount)
          setTimeout(function () {
            that.doGenerateQRCode(signCode, retryCount - 1)
          }, 500)
          return
        }

        var canvas = res[0].node
        var canvasWidth = res[0].width
        var canvasHeight = res[0].height
        var dpr = wx.getWindowInfo().pixelRatio

        canvas.width = canvasWidth * dpr
        canvas.height = canvasHeight * dpr

        console.log('Canvas 显示尺寸:', canvasWidth, canvasHeight, 'dpr:', dpr)

        var ctx = canvas.getContext('2d')
        ctx.scale(dpr, dpr)

        var padding = 20
        var typeNumber = 4
        var errorCorrectionLevel = 0

        var qr = QRCode.createQRCode(signCode, typeNumber, errorCorrectionLevel)
        console.log('QRCode 生成结果:', qr, 'moduleCount:', qr ? qr.getModuleCount() : 'null')

        var moduleCount = qr.getModuleCount()
        var qrSize = Math.min(canvasWidth, canvasHeight) - padding * 2
        var cellSize = qrSize / moduleCount

        ctx.fillStyle = '#ffffff'
        ctx.fillRect(0, 0, canvasWidth, canvasHeight)

        var offsetX = (canvasWidth - qrSize) / 2
        var offsetY = (canvasHeight - qrSize) / 2

        for (var row = 0; row < moduleCount; row++) {
          for (var col = 0; col < moduleCount; col++) {
            ctx.fillStyle = qr.isDark(row, col) ? '#000000' : '#ffffff'
            ctx.fillRect(col * cellSize + offsetX, row * cellSize + offsetY, cellSize, cellSize)
          }
        }

        console.log('二维码绘制完成')
      })
  },

  startQRCodeCountdown() {
    this.data.qrcodeTimer = setInterval(() => {
      let countdown = this.data.qrcodeCountdown - 1
      if (countdown <= 0) {
        this.endQRCodeSign()
      } else {
        this.setData({ qrcodeCountdown: countdown })
      }
    }, 1000)
  },

  startLocationCountdown() {
    this.data.locationTimer = setInterval(() => {
      let countdown = this.data.locationCountdown - 1
      if (countdown <= 0) {
        this.endLocationSign()
      } else {
        this.setData({ locationCountdown: countdown })
      }
    }, 1000)
  },

  startQRCodePolling() {
    this.loadQRCodeStatistics()
    this.data.qrcodePollingTimer = setInterval(() => {
      this.loadQRCodeStatistics()
    }, 3000)
  },

  startLocationPolling() {
    this.loadLocationStatistics()
    this.data.locationPollingTimer = setInterval(() => {
      this.loadLocationStatistics()
    }, 3000)
  },

  async loadQRCodeStatistics() {
    try {
      const res = await get('/attendance/statistics', {
        courseId: this.data.selectedCourse.courseId
      })
      if (res.success) {
        this.setData({
          qrcodeSignedCount: res.data.signed,
          qrcodeUnsignedCount: res.data.unsigned + res.data.absent
        })
      }
    } catch (err) {
      console.error(err)
    }
  },

  async loadLocationStatistics() {
    try {
      const res = await get('/attendance/statistics', {
        courseId: this.data.selectedCourse.courseId
      })
      if (res.success) {
        this.setData({
          locationSignedCount: res.data.signed,
          locationUnsignedCount: res.data.unsigned + res.data.absent
        })
      }
    } catch (err) {
      console.error(err)
    }
  },

  async endQRCodeSign() {
    this.clearQRCodeTimers()

    wx.showLoading({ title: '结束签到...' })
    try {
      const res = await post('/attendance/end', {
        courseId: this.data.selectedCourse.courseId
      })
      wx.hideLoading()

      if (res.success) {
        app.clearQRCodeSignState(this.data.selectedCourse.courseId)

        wx.showToast({ title: '签到已结束', icon: 'success' })
        this.setData({
          isQRCodeSigning: false,
          qrcodeSignCode: '',
          qrcodeCountdown: 0
        })
      }
    } catch (err) {
      wx.hideLoading()
      wx.showToast({ title: '网络错误', icon: 'none' })
    }
  },

  async endLocationSign() {
    this.clearLocationTimers()

    wx.showLoading({ title: '结束签到...' })
    try {
      const res = await post('/attendance/end', {
        courseId: this.data.selectedCourse.courseId
      })
      wx.hideLoading()

      if (res.success) {
        app.clearLocationSignState(this.data.selectedCourse.courseId)

        wx.showToast({ title: '签到已结束', icon: 'success' })
        this.setData({
          isLocationSigning: false,
          locationSignCode: '',
          locationCountdown: 0,
          teacherLocation: '',
          teacherLatitude: null,
          teacherLongitude: null
        })
      }
    } catch (err) {
      wx.hideLoading()
      wx.showToast({ title: '网络错误', icon: 'none' })
    }
  },

  async endSign() {
    const { signType } = this.data
    if (signType === 'location') {
      await this.endLocationSign()
    } else {
      await this.endQRCodeSign()
    }
  },

  clearQRCodeTimers() {
    if (this.data.qrcodeTimer) {
      clearInterval(this.data.qrcodeTimer)
      this.data.qrcodeTimer = null
    }
    if (this.data.qrcodePollingTimer) {
      clearInterval(this.data.qrcodePollingTimer)
      this.data.qrcodePollingTimer = null
    }
  },

  clearLocationTimers() {
    if (this.data.locationTimer) {
      clearInterval(this.data.locationTimer)
      this.data.locationTimer = null
    }
    if (this.data.locationPollingTimer) {
      clearInterval(this.data.locationPollingTimer)
      this.data.locationPollingTimer = null
    }
  },

  clearDigitalTimers() {
    if (this.data.digitalTimer) {
      clearInterval(this.data.digitalTimer)
      this.data.digitalTimer = null
    }
    if (this.data.digitalPollingTimer) {
      clearInterval(this.data.digitalPollingTimer)
      this.data.digitalPollingTimer = null
    }
  },

  clearAllTimers() {
    this.clearQRCodeTimers()
    this.clearLocationTimers()
    this.clearDigitalTimers()
  },

  async startDigitalSign() {
    const { selectedCourse } = this.data
    if (!selectedCourse) {
      wx.showToast({ title: '请选择课程', icon: 'none' })
      return
    }

    wx.showLoading({ title: '生成口令...' })
    try {
      const res = await post('/digital/generate', {
        courseId: selectedCourse.courseId,
        teacherId: this.data.userInfo.teacherId,
        expireSeconds: 60
      })
      wx.hideLoading()

      if (res.success) {
        const token = res.data.token
        this.setData({
          isDigitalSigning: true,
          digitalToken: token.split(''),
          digitalCountdown: 60,
          digitalSignedCount: 0,
          digitalUnsignedCount: 0
        })

        this.startDigitalCountdown()
        this.startDigitalPolling()
        wx.showToast({ title: '口令已生成', icon: 'success' })
      } else {
        wx.showToast({ title: res.message, icon: 'none' })
      }
    } catch (err) {
      wx.hideLoading()
      wx.showToast({ title: '网络错误', icon: 'none' })
    }
  },

  startDigitalCountdown() {
    this.data.digitalTimer = setInterval(() => {
      let countdown = this.data.digitalCountdown - 1
      if (countdown <= 0) {
        this.endDigitalSign()
      } else {
        this.setData({ digitalCountdown: countdown })
      }
    }, 1000)
  },

  startDigitalPolling() {
    this.loadDigitalStatistics()
    this.data.digitalPollingTimer = setInterval(() => {
      this.loadDigitalStatistics()
    }, 3000)
  },

  async loadDigitalStatistics() {
    try {
      const res = await get('/attendance/statistics', {
        courseId: this.data.selectedCourse.courseId
      })
      if (res.success) {
        this.setData({
          digitalSignedCount: res.data.signed,
          digitalUnsignedCount: res.data.unsigned + res.data.absent
        })
      }
    } catch (err) {
      console.error(err)
    }
  },

  async endDigitalSign() {
    this.clearDigitalTimers()

    wx.showLoading({ title: '结束签到...' })
    try {
      await post('/digital/expire', {
        courseId: this.data.selectedCourse.courseId
      })
      await post('/attendance/end', {
        courseId: this.data.selectedCourse.courseId
      })
      wx.hideLoading()

      wx.showToast({ title: '签到已结束', icon: 'success' })
      this.setData({
        isDigitalSigning: false,
        digitalToken: '',
        digitalCountdown: 0
      })
    } catch (err) {
      wx.hideLoading()
      wx.showToast({ title: '网络错误', icon: 'none' })
    }
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
    if (index !== 2) {
      wx.redirectTo({ url: urls[index] })
    }
  },

  async createSignReminder(signCode, courseId) {
    try {
      await post('/signReminder/create', {
        signCode: signCode,
        courseId: courseId,
        teacherId: this.data.userInfo.teacherId
      })
    } catch (err) {
      console.error('创建签到提醒失败:', err)
    }
  }
})

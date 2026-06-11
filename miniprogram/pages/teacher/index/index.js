const app = getApp()
const { get, post } = require('../../../utils/api')

Page({
  data: {
    userInfo: {},
    courses: [],
    stats: {
      totalCourses: 0,
      todaySign: 0,
      totalStudents: 0
    },
    pendingCount: 0,
    aiQuestion: '',
    aiAnswer: '',
    aiLoading: false,
    aiHistory: []
  },

  onLoad() {
    this.checkLogin()
  },

  onShow() {
    this.loadCourses()
    this.loadDashboardStats()
    this.loadPendingCount()
  },

  checkLogin() {
    const userInfo = wx.getStorageSync('userInfo')
    const userType = wx.getStorageSync('userType')
    if (!userInfo || userType !== 'teacher') {
      wx.redirectTo({ url: '/pages/login/login' })
    } else {
      this.setData({ userInfo })
      app.globalData.userInfo = userInfo
      app.globalData.userType = userType
    }
  },

  async loadCourses() {
    try {
      const res = await get('/course/list', { teacherId: this.data.userInfo.teacherId })
      if (res.success) {
        this.setData({ courses: res.data })
      }
    } catch (err) {
      console.error(err)
    }
  },

  async loadDashboardStats() {
    try {
      const res = await get('/attendance/teacherDashboard', { teacherId: this.data.userInfo.teacherId })
      if (res.success) {
        this.setData({
          'stats.totalCourses': res.data.totalCourses || 0,
          'stats.todaySign': res.data.todaySign || 0,
          'stats.totalStudents': res.data.totalStudents || 0
        })
      }
    } catch (err) {
      console.error(err)
    }
  },

  async loadPendingCount() {
    try {
      const res = await get('/application/pendingCount', { teacherId: this.data.userInfo.teacherId })
      if (res.success) {
        this.setData({ pendingCount: res.data.count })
      }
    } catch (err) {
      console.error(err)
    }
  },

  onAiInputChange(e) {
    this.setData({ aiQuestion: e.detail.value })
  },

  async submitAiQuestion() {
    const question = this.data.aiQuestion.trim()
    if (!question) {
      wx.showToast({ title: '请输入问题', icon: 'none' })
      return
    }
    if (this.data.aiLoading) return

    this.setData({ aiLoading: true, aiAnswer: '' })

    try {
      const res = await post('/attendance/ai/query', { question })
      if (res.success) {
        this.setData({
          aiAnswer: res.data,
          aiQuestion: '',
          aiHistory: [{ question, answer: res.data, time: this.formatAiTime() }, ...this.data.aiHistory].slice(0, 5)
        })
      } else {
        this.setData({ aiAnswer: '查询失败：' + (res.message || 'AI服务暂时不可用') })
      }
    } catch (err) {
      console.error(err)
      this.setData({ aiAnswer: '网络错误，请检查后端服务或稍后重试' })
    } finally {
      this.setData({ aiLoading: false })
    }
  },

  useAiSuggestion(e) {
    const question = e.currentTarget.dataset.question
    this.setData({ aiQuestion: question })
    this.submitAiQuestion()
  },

  clearAiHistory() {
    this.setData({ aiHistory: [], aiAnswer: '', aiQuestion: '' })
  },

  formatAiTime() {
    const now = new Date()
    const h = now.getHours().toString().padStart(2, '0')
    const m = now.getMinutes().toString().padStart(2, '0')
    return h + ':' + m
  },

  goToSign(e) {
    const course = e.currentTarget.dataset.course
    wx.navigateTo({ 
      url: `/pages/teacher/sign/sign?courseId=${course.courseId}&courseName=${course.courseName}&className=${course.className}` 
    })
  },

  goToApproval() {
    wx.navigateTo({ url: '/pages/teacher/approve/approve' })
  },

  goToRoster() {
    const courses = this.data.courses
    if (courses.length === 0) {
      wx.showToast({ title: '暂无课程', icon: 'none' })
      return
    }
    const firstCourse = courses[0]
    wx.navigateTo({
      url: `/pages/teacher/roster/roster?className=${firstCourse.className}&courseId=${firstCourse.courseId}`
    })
  },

  goToHistory() {
    wx.navigateTo({ url: '/pages/teacher/history/history' })
  },

  goToSchedule() {
    wx.navigateTo({ url: '/pages/teacher/schedule/schedule' })
  },

  goToExport() {
    const teacherId = this.data.userInfo.teacherId
    wx.showLoading({ title: '生成报表中...' })
    wx.downloadFile({
      url: `http://localhost:8081/api/export/monthlyAttendance?teacherId=${teacherId}`,
      success(res) {
        wx.hideLoading()
        if (res.statusCode === 200) {
          wx.openDocument({
            filePath: res.tempFilePath,
            showMenu: true,
            success() {
              wx.showToast({ title: '报表已打开', icon: 'success' })
            },
            fail() {
              wx.showToast({ title: '打开文件失败', icon: 'none' })
            }
          })
        } else {
          wx.showToast({ title: '导出失败', icon: 'none' })
        }
      },
      fail() {
        wx.hideLoading()
        wx.showToast({ title: '导出失败，请检查网络', icon: 'none' })
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
    if (index !== 0) {
      wx.redirectTo({ url: urls[index] })
    }
  }
})

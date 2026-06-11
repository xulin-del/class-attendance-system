const app = getApp()
const { get, post } = require('../../../utils/api')

Page({
  data: {
    userInfo: {},
    stats: {
      total: 0,
      signed: 0,
      late: 0,
      absent: 0,
      rate: 0
    },
    records: [],
    page: 1,
    pageSize: 10,
    hasMore: true,
    loading: false,
    signReminders: [],
    classReminders: [],
    courseSignReminders: [],
    showClassReminderModal: false,
    currentClassReminder: {},
    aiQuestion: '',
    aiAnswer: '',
    aiLoading: false,
    aiHistory: []
  },

  onLoad() {
    this.checkLogin()
  },

  onShow() {
    this.loadStats()
    this.loadRecords()
    this.loadSignReminders()
    this.startReminderPolling()
  },

  onHide() {
    this.stopReminderPolling()
  },

  onUnload() {
    this.stopReminderPolling()
  },

  onPullDownRefresh() {
    this.setData({ page: 1, hasMore: true, records: [] })
    Promise.all([
      this.loadStats(),
      this.loadRecords(),
      this.loadSignReminders()
    ]).then(() => {
      wx.stopPullDownRefresh()
    })
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadRecords()
    }
  },

  checkLogin() {
    const userInfo = wx.getStorageSync('userInfo')
    const userType = wx.getStorageSync('userType')
    if (!userInfo || userType !== 'student') {
      wx.redirectTo({ url: '/pages/login/login' })
    } else {
      this.setData({ userInfo })
      app.globalData.userInfo = userInfo
      app.globalData.userType = userType
    }
  },

  async loadStats() {
    try {
      const res = await get('/attendance/studentStats', { studentId: this.data.userInfo.studentId })
      if (res.success) {
        const stats = res.data
        this.setData({ stats })
        this.drawProgressRing(stats.rate)
      }
    } catch (err) {
      console.error(err)
    }
  },

  async loadRecords() {
    if (this.data.loading) return
    
    this.setData({ loading: true })
    try {
      const res = await get('/attendance/studentRecordsPage', {
        studentId: this.data.userInfo.studentId,
        page: this.data.page,
        pageSize: this.data.pageSize
      })
      if (res.success) {
        const newRecords = res.data.records || []
        const records = this.data.page === 1 ? newRecords : [...this.data.records, ...newRecords]
        const hasMore = records.length < res.data.total
        
        this.setData({
          records,
          hasMore,
          page: this.data.page + 1,
          loading: false
        })
      }
    } catch (err) {
      console.error(err)
      this.setData({ loading: false })
    }
  },

  drawProgressRing(rate) {
    const that = this
    setTimeout(() => {
      wx.createSelectorQuery()
        .select('#progressCanvas')
        .fields({ node: true, size: true })
        .exec((res) => {
          if (!res || !res[0] || !res[0].node) return

          const canvas = res[0].node
          const ctx = canvas.getContext('2d')
          const dpr = wx.getWindowInfo().pixelRatio
          const width = res[0].width
          const height = res[0].height

          canvas.width = width * dpr
          canvas.height = height * dpr
          ctx.scale(dpr, dpr)

          const centerX = width / 2
          const centerY = height / 2
          const radius = Math.min(width, height) / 2 - 15
          const lineWidth = 12

          let color = '#2f8f56'
          if (rate < 60) {
            color = '#d94841'
          } else if (rate < 90) {
            color = '#c47a1b'
          }

          ctx.beginPath()
          ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI)
          ctx.strokeStyle = '#edf0eb'
          ctx.lineWidth = lineWidth
          ctx.stroke()

          const progress = rate / 100
          const endAngle = -Math.PI / 2 + 2 * Math.PI * progress

          ctx.beginPath()
          ctx.arc(centerX, centerY, radius, -Math.PI / 2, endAngle)
          ctx.strokeStyle = color
          ctx.lineWidth = lineWidth
          ctx.lineCap = 'round'
          ctx.stroke()
        })
    }, 100)
  },

  goToRecords(e) {
    const status = e.currentTarget.dataset.status || ''
    wx.navigateTo({ 
      url: `/pages/student/records/records?status=${status}` 
    })
  },

  async loadSignReminders() {
    try {
      const res = await get('/signReminder/pending', { studentId: this.data.userInfo.studentId })
      if (res.success) {
        const signReminders = res.data || []
        const classReminders = signReminders.filter(item => item.reminderType === 'class')
        const courseSignReminders = signReminders.filter(item => item.reminderType !== 'class')
        this.setData({
          signReminders,
          classReminders,
          courseSignReminders
        })
      }
    } catch (err) {
      console.error('加载签到提醒失败:', err)
    }
  },

  startReminderPolling() {
    this.stopReminderPolling()
    this.reminderTimer = setInterval(() => {
      if (this.data.userInfo && this.data.userInfo.studentId) {
        this.loadSignReminders()
      }
    }, 60000)
  },

  stopReminderPolling() {
    if (this.reminderTimer) {
      clearInterval(this.reminderTimer)
      this.reminderTimer = null
    }
  },

  onClassReminderTap() {
    const reminders = this.data.classReminders
    if (reminders && reminders.length > 0) {
      const reminder = reminders[0]
      this.showClassReminderDetail(reminder)
      this.consumeReminder(reminder)
    }
  },

  onCourseSignReminderTap() {
    const reminders = this.data.courseSignReminders
    if (reminders && reminders.length > 0) {
      const reminder = reminders[0]
      this.consumeReminder(reminder)
      wx.navigateTo({
        url: `/pages/student/sign/sign?signCode=${reminder.signCode}`
      })
    }
  },

  goToSignFromBanner() {
    this.onCourseSignReminderTap()
  },

  async consumeReminder(reminder) {
    if (!reminder || !reminder.id) return

    const signReminders = this.data.signReminders.filter(item => item.id !== reminder.id)
    this.setData({
      signReminders,
      classReminders: signReminders.filter(item => item.reminderType === 'class'),
      courseSignReminders: signReminders.filter(item => item.reminderType !== 'class')
    })

    try {
      await post('/signReminder/dismiss', { id: reminder.id })
    } catch (err) {
      console.error('更新提醒状态失败:', err)
    }
  },

  showClassReminderDetail(reminder) {
    const date = this.formatReminderDate(reminder.reminderDate)
    this.setData({
      currentClassReminder: {
        courseName: reminder.courseName || '未填写',
        className: reminder.className || '未填写',
        location: reminder.location || '未填写',
        time: `${date} ${reminder.startTime || '--'}-${reminder.endTime || '--'}`,
        teacherName: reminder.teacherName || reminder.teacherId || '未填写'
      },
      showClassReminderModal: true
    })
  },

  closeClassReminderModal() {
    this.setData({
      showClassReminderModal: false,
      currentClassReminder: {}
    }, () => {
      this.drawProgressRing(this.data.stats.rate || 0)
    })
  },

  noop() {},

  formatReminderDate(value) {
    if (!value) return '今天'
    if (typeof value === 'string' && value.includes('T')) {
      return value.slice(0, 10)
    }
    const normalized = typeof value === 'string' ? value.replace(/-/g, '/') : value
    const date = new Date(normalized)
    if (Number.isNaN(date.getTime())) {
      return value
    }
    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    return `${year}-${month}-${day}`
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

  switchTab(e) {
    const index = e.currentTarget.dataset.index
    const urls = [
      '/pages/student/index/index',
      '/pages/student/sign/sign',
      '/pages/student/records/records',
      '/pages/student/mine/mine'
    ]
    if (index !== 0) {
      wx.redirectTo({ url: urls[index] })
    }
  }
})

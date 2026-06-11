const { get, post } = require('../../../utils/api')

const COLORS = [
  'linear-gradient(135deg, #1677FF, #69b1ff)',
  'linear-gradient(135deg, #52c41a, #95de64)',
  'linear-gradient(135deg, #fa8c16, #ffc53d)',
  'linear-gradient(135deg, #722ed1, #b37feb)',
  'linear-gradient(135deg, #eb2f96, #ff85c0)',
  'linear-gradient(135deg, #13c2c2, #5cdbd3)',
  'linear-gradient(135deg, #fa541c, #ff7a45)'
]

const SOLID_COLORS = [
  '#1677FF', '#52c41a', '#fa8c16', '#722ed1', '#eb2f96', '#13c2c2', '#fa541c'
]

Page({
  data: {
    schedules: [],
    scheduleGrid: [],
    courses: [],
    weekDays: [],
    timeSlots: ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00'],
    weekDayNames: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    showModal: false,
    isEdit: false,
    editId: null,
    form: {
      courseIndex: 0,
      dayOfWeek: 1,
      startTime: '08:00',
      endTime: '09:40',
      location: '',
      weekStart: 1,
      weekEnd: 16
    }
  },

  onLoad() {
    this.initWeekDays()
    this.loadCourses()
    this.loadSchedules()
  },

  initWeekDays() {
    const now = new Date()
    const day = now.getDay()
    const monday = new Date(now)
    monday.setDate(now.getDate() - (day === 0 ? 6 : day - 1))

    const weekDays = []
    const names = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    for (let i = 0; i < 7; i++) {
      const d = new Date(monday)
      d.setDate(monday.getDate() + i)
      weekDays.push({
        day: i + 1,
        name: names[i],
        date: `${d.getMonth() + 1}/${d.getDate()}`
      })
    }
    this.setData({ weekDays })
  },

  async loadCourses() {
    const userInfo = wx.getStorageSync('userInfo')
    try {
      const res = await get('/course/list', { teacherId: userInfo.teacherId })
      if (res.success) {
        this.setData({ courses: res.data })
      }
    } catch (err) {
      console.error(err)
    }
  },

  async loadSchedules() {
    const userInfo = wx.getStorageSync('userInfo')
    try {
      const res = await get('/schedule/list', { teacherId: userInfo.teacherId })
      if (res.success) {
        const schedules = res.data.map((item, index) => ({
          ...item,
          color: SOLID_COLORS[index % SOLID_COLORS.length]
        }))
        const scheduleGrid = this.buildScheduleGrid(schedules)
        this.setData({ schedules, scheduleGrid })
      }
    } catch (err) {
      console.error(err)
    }
  },

  buildScheduleGrid(schedules) {
    const grid = []
    for (let day = 1; day <= 7; day++) {
      const dayColumn = []
      for (let timeIndex = 0; timeIndex < 7; timeIndex++) {
        let found = null
        for (const s of schedules) {
          if (s.dayOfWeek === day) {
            const hour = parseInt(s.startTime.split(':')[0])
            if (hour >= timeIndex * 2 + 8 && hour < (timeIndex + 1) * 2 + 8) {
              found = s
              break
            }
          }
        }
        dayColumn.push(found)
      }
      grid.push(dayColumn)
    }
    return grid
  },

  showAddModal() {
    this.setData({
      showModal: true,
      isEdit: false,
      editId: null,
      form: {
        courseIndex: 0,
        dayOfWeek: 1,
        startTime: '08:00',
        endTime: '09:40',
        location: '',
        weekStart: 1,
        weekEnd: 16
      }
    })
  },

  hideModal() {
    this.setData({ showModal: false })
  },

  onCourseChange(e) {
    this.setData({ 'form.courseIndex': e.detail.value })
  },

  onDayChange(e) {
    this.setData({ 'form.dayOfWeek': parseInt(e.detail.value) + 1 })
  },

  onStartTimeChange(e) {
    this.setData({ 'form.startTime': e.detail.value })
  },

  onEndTimeChange(e) {
    this.setData({ 'form.endTime': e.detail.value })
  },

  onLocationInput(e) {
    this.setData({ 'form.location': e.detail.value })
  },

  onWeekStartInput(e) {
    this.setData({ 'form.weekStart': parseInt(e.detail.value) || 1 })
  },

  onWeekEndInput(e) {
    this.setData({ 'form.weekEnd': parseInt(e.detail.value) || 16 })
  },

  async submitSchedule() {
    const { form, courses, isEdit, editId } = this.data
    if (courses.length === 0) {
      wx.showToast({ title: '请先添加课程', icon: 'none' })
      return
    }

    const selectedCourse = courses[form.courseIndex]
    const data = {
      courseId: selectedCourse.courseId,
      teacherId: wx.getStorageSync('userInfo').teacherId,
      dayOfWeek: form.dayOfWeek,
      startTime: form.startTime,
      endTime: form.endTime,
      location: form.location,
      weekStart: form.weekStart,
      weekEnd: form.weekEnd
    }

    try {
      let res
      if (isEdit) {
        data.id = editId
        res = await post('/schedule/update', data)
      } else {
        res = await post('/schedule/add', data)
      }
      if (res.success) {
        wx.showToast({ title: isEdit ? '更新成功' : '添加成功', icon: 'success' })
        this.hideModal()
        this.loadSchedules()
      } else {
        wx.showToast({ title: res.message || '操作失败', icon: 'none' })
      }
    } catch (err) {
      console.error(err)
      wx.showToast({ title: '操作失败', icon: 'none' })
    }
  },

  onScheduleTap(e) {
    const schedule = e.currentTarget.dataset.schedule
    if (!schedule) return
    wx.showActionSheet({
      itemList: ['编辑', '删除'],
      success: (res) => {
        if (res.tapIndex === 0) {
          this.onEditSchedule({ currentTarget: { dataset: { schedule } } })
        } else if (res.tapIndex === 1) {
          this.onDeleteSchedule({ currentTarget: { dataset: { schedule } } })
        }
      }
    })
  },

  onEditSchedule(e) {
    const schedule = e.currentTarget.dataset.schedule
    const courseIndex = this.data.courses.findIndex(c => c.courseId === schedule.courseId)
    this.setData({
      showModal: true,
      isEdit: true,
      editId: schedule.id,
      form: {
        courseIndex: courseIndex >= 0 ? courseIndex : 0,
        dayOfWeek: schedule.dayOfWeek,
        startTime: schedule.startTime,
        endTime: schedule.endTime,
        location: schedule.location || '',
        weekStart: schedule.weekStart || 1,
        weekEnd: schedule.weekEnd || 16
      }
    })
  },

  onDeleteSchedule(e) {
    const schedule = e.currentTarget.dataset.schedule
    wx.showModal({
      title: '确认删除',
      content: `确定删除"${schedule.courseName}"的课表安排吗？`,
      success: async (res) => {
        if (res.confirm) {
          try {
            const result = await post('/schedule/delete', { id: schedule.id })
            if (result.success) {
              wx.showToast({ title: '删除成功', icon: 'success' })
              this.loadSchedules()
            } else {
              wx.showToast({ title: '删除失败', icon: 'none' })
            }
          } catch (err) {
            console.error(err)
          }
        }
      }
    })
  }
})

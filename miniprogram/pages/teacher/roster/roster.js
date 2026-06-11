const { get } = require('../../../utils/api')

Page({
  data: {
    courses: [],
    classNames: [],
    classIndex: 0,
    students: [],
    currentClassName: ''
  },

  onLoad(options) {
    const className = options.className || ''
    const courseId = options.courseId || ''
    this.setData({ currentClassName: className })
    this.loadCourses(className)
  },

  async loadCourses(defaultClassName) {
    const userInfo = wx.getStorageSync('userInfo')
    try {
      const res = await get('/course/list', { teacherId: userInfo.teacherId })
      if (res.success) {
        const courses = res.data
        const classNames = [...new Set(courses.map(c => c.className))]
        const classIndex = classNames.indexOf(defaultClassName)
        this.setData({
          courses,
          classNames,
          classIndex: classIndex >= 0 ? classIndex : 0
        })
        this.loadStudents(classNames[this.data.classIndex])
      }
    } catch (err) {
      console.error(err)
    }
  },

  async loadStudents(className) {
    try {
      const res = await get('/student/list', { className })
      if (res.success) {
        this.setData({ students: res.data })
      }
    } catch (err) {
      console.error(err)
    }
  },

  onClassChange(e) {
    const classIndex = e.detail.value
    this.setData({ classIndex })
    this.loadStudents(this.data.classNames[classIndex])
  }
})

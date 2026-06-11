const app = getApp()
const { get, post } = require('../../../utils/api')

Page({
  data: {
    userInfo: {},
    courses: [],
    showModal: false,
    isEdit: false,
    formData: {
      courseId: null,
      courseName: '',
      className: '',
      teacherId: ''
    }
  },

  onLoad() {
    this.checkLogin()
  },

  onShow() {
    this.loadCourses()
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
        this.setData({ courses: res.data })
      }
    } catch (err) {
      console.error(err)
    }
  },

  showAddModal() {
    this.setData({
      showModal: true,
      isEdit: false,
      formData: {
        courseId: null,
        courseName: '',
        className: '',
        teacherId: this.data.userInfo.teacherId
      }
    })
  },

  hideModal() {
    this.setData({ showModal: false })
  },

  onCourseNameInput(e) {
    this.setData({ 'formData.courseName': e.detail.value })
  },

  onClassNameInput(e) {
    this.setData({ 'formData.className': e.detail.value })
  },

  editCourse(e) {
    const course = e.currentTarget.dataset.course
    this.setData({
      showModal: true,
      isEdit: true,
      formData: {
        courseId: course.courseId,
        courseName: course.courseName,
        className: course.className,
        teacherId: course.teacherId
      }
    })
  },

  async submitCourse() {
    const { formData, isEdit } = this.data
    if (!formData.courseName || !formData.className) {
      wx.showToast({ title: '请填写完整信息', icon: 'none' })
      return
    }

    wx.showLoading({ title: '提交中...' })
    try {
      let res
      if (isEdit) {
        res = await post('/course/update', {
          courseId: formData.courseId,
          courseName: formData.courseName,
          className: formData.className,
          teacherId: formData.teacherId
        })
      } else {
        res = await post('/course/add', {
          courseName: formData.courseName,
          className: formData.className,
          teacherId: formData.teacherId
        })
      }
      wx.hideLoading()
      
      if (res.success) {
        wx.showToast({ title: res.message, icon: 'success' })
        this.hideModal()
        this.loadCourses()
      } else {
        wx.showToast({ title: res.message, icon: 'none' })
      }
    } catch (err) {
      wx.hideLoading()
      wx.showToast({ title: '网络错误', icon: 'none' })
    }
  },

  deleteCourse(e) {
    const courseId = e.currentTarget.dataset.id
    wx.showModal({
      title: '提示',
      content: '确定要删除该课程吗？',
      success: async (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '删除中...' })
          try {
            const result = await post('/course/delete', { courseId })
            wx.hideLoading()
            if (result.success) {
              wx.showToast({ title: '删除成功', icon: 'success' })
              this.loadCourses()
            } else {
              wx.showToast({ title: result.message, icon: 'none' })
            }
          } catch (err) {
            wx.hideLoading()
            wx.showToast({ title: '网络错误', icon: 'none' })
          }
        }
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
    if (index !== 1) {
      wx.redirectTo({ url: urls[index] })
    }
  }
})
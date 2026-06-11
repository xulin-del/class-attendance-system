const app = getApp()
const { get, post } = require('../../../utils/api')

function getStatusInfo(status) {
  const statusMap = {
    '已签到': { class: 'status-signed', text: '已签到' },
    '未签到': { class: 'status-unsigned', text: '未签到' },
    '缺勤': { class: 'status-absent', text: '缺勤' },
    '已补签': { class: 'status-signed', text: '已补签' }
  }
  return statusMap[status] || { class: 'status-unsigned', text: status }
}

Page({
  data: {
    userInfo: {},
    records: [],
    stats: {
      signed: 0,
      absent: 0,
      total: 0
    },
    showApplyModal: false,
    currentRecord: null,
    applyReason: '',
    applyImages: [],
    makeupMode: false
  },

  onLoad(options) {
    this.setData({ makeupMode: options && options.makeup === '1' })
    this.checkLogin()
  },

  onShow() {
    this.loadRecords()
    if (this.data.makeupMode) {
      wx.showToast({ title: '请选择未签到或缺勤记录补签', icon: 'none' })
    }
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

  async loadRecords() {
    try {
      const res = await get('/attendance/studentRecords', { 
        studentId: this.data.userInfo.studentId 
      })
      if (res.success) {
        let signed = 0, absent = 0
        const records = res.data.map(item => {
          if (item.status === '已签到' || item.status === '已补签') signed++
          if (item.status === '缺勤') absent++
          const statusInfo = getStatusInfo(item.status)
          return {
            ...item,
            statusClass: statusInfo.class,
            statusText: statusInfo.text,
            applicationStatus: null
          }
        })
        
        this.setData({ 
          records: records,
          stats: {
            signed,
            absent,
            total: res.data.length
          }
        })
        
        this.checkApplications()
      }
    } catch (err) {
      console.error(err)
    }
  },

  async checkApplications() {
    try {
      const res = await get('/application/studentList', { 
        studentId: this.data.userInfo.studentId 
      })
      if (res.success) {
        const applications = res.data
        const records = this.data.records.map(record => {
          const app = applications.find(a => a.recordId === record.recordId)
          if (app) {
            record.applicationStatus = app.status
          }
          return record
        })
        this.setData({ records })
      }
    } catch (err) {
      console.error(err)
    }
  },

  showApplyModal(e) {
    const record = e.currentTarget.dataset.record
    if (record.applicationStatus) {
      return
    }
    
    this.setData({
      showApplyModal: true,
      currentRecord: record,
      applyReason: '',
      applyImages: []
    })
  },

  hideApplyModal() {
    this.setData({
      showApplyModal: false,
      currentRecord: null,
      applyReason: '',
      applyImages: []
    })
  },

  onReasonInput(e) {
    this.setData({ applyReason: e.detail.value })
  },

  chooseImage() {
    const that = this
    wx.chooseImage({
      count: 3 - this.data.applyImages.length,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success(res) {
        that.setData({
          applyImages: that.data.applyImages.concat(res.tempFilePaths)
        })
      }
    })
  },

  removeImage(e) {
    const index = e.currentTarget.dataset.index
    const images = this.data.applyImages.filter((_, i) => i !== index)
    this.setData({ applyImages: images })
  },

  previewImage(e) {
    const url = e.currentTarget.dataset.url
    wx.previewImage({
      current: url,
      urls: this.data.applyImages
    })
  },

  async submitApplication() {
    if (!this.data.applyReason.trim()) {
      wx.showToast({ title: '请输入申请理由', icon: 'none' })
      return
    }
    
    const record = this.data.currentRecord
    wx.showLoading({ title: '提交中...' })
    
    try {
      let imagesStr = ''
      if (this.data.applyImages.length > 0) {
        imagesStr = this.data.applyImages.join(',')
      }
      
      const res = await post('/application/submit', {
        recordId: record.recordId,
        courseId: record.courseId,
        studentId: this.data.userInfo.studentId,
        courseName: record.courseName || '',
        absenceDate: record.absenceDate || '',
        reason: this.data.applyReason,
        images: imagesStr
      })
      
      wx.hideLoading()
      
      if (res.success) {
        wx.showToast({ title: '提交成功', icon: 'success' })
        this.hideApplyModal()
        this.loadRecords()
      } else {
        wx.showToast({ title: res.message, icon: 'none' })
      }
    } catch (err) {
      wx.hideLoading()
      wx.showToast({ title: '提交失败', icon: 'none' })
    }
  },

  switchTab(e) {
    const index = e.currentTarget.dataset.index
    const urls = [
      '/pages/student/index/index',
      '/pages/student/sign/sign',
      '/pages/student/records/records',
      '/pages/student/mine/mine'
    ]
    if (index !== 2) {
      wx.redirectTo({ url: urls[index] })
    }
  }
})

const app = getApp()
const { get, post } = require('../../../utils/api')

Page({
  data: {
    userInfo: {},
    applications: [],
    showRejectModal: false,
    currentApplicationId: null,
    rejectReason: ''
  },

  onLoad() {
    this.checkLogin()
  },

  onShow() {
    this.loadApplications()
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

  async loadApplications() {
    try {
      const res = await get('/application/pending', { teacherId: this.data.userInfo.teacherId })
      if (res.success) {
        const applications = res.data.map(item => {
          let imageList = []
          if (item.images) {
            imageList = item.images.split(',').filter(img => img.trim())
          }
          return {
            ...item,
            imageList
          }
        })
        this.setData({ applications })
      }
    } catch (err) {
      console.error(err)
    }
  },

  previewImage(e) {
    const url = e.currentTarget.dataset.url
    const urls = e.currentTarget.dataset.urls
    wx.previewImage({
      current: url,
      urls: urls
    })
  },

  async approveApplication(e) {
    const applicationId = e.currentTarget.dataset.id
    
    wx.showModal({
      title: '确认通过',
      content: '确定通过该补签申请吗？',
      success: async (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '处理中...' })
          try {
            const result = await post('/application/approve', { applicationId })
            wx.hideLoading()
            
            if (result.success) {
              wx.showToast({ title: '已通过', icon: 'success' })
              this.loadApplications()
            } else {
              wx.showToast({ title: result.message, icon: 'none' })
            }
          } catch (err) {
            wx.hideLoading()
            wx.showToast({ title: '操作失败', icon: 'none' })
          }
        }
      }
    })
  },

  showRejectModal(e) {
    const applicationId = e.currentTarget.dataset.id
    this.setData({
      showRejectModal: true,
      currentApplicationId: applicationId,
      rejectReason: ''
    })
  },

  hideRejectModal() {
    this.setData({
      showRejectModal: false,
      currentApplicationId: null,
      rejectReason: ''
    })
  },

  onRejectReasonInput(e) {
    this.setData({ rejectReason: e.detail.value })
  },

  async rejectApplication() {
    const applicationId = this.data.currentApplicationId
    const rejectReason = this.data.rejectReason
    
    wx.showLoading({ title: '处理中...' })
    try {
      const result = await post('/application/reject', { 
        applicationId,
        rejectReason
      })
      wx.hideLoading()
      
      if (result.success) {
        wx.showToast({ title: '已拒绝', icon: 'success' })
        this.hideRejectModal()
        this.loadApplications()
      } else {
        wx.showToast({ title: result.message, icon: 'none' })
      }
    } catch (err) {
      wx.hideLoading()
      wx.showToast({ title: '操作失败', icon: 'none' })
    }
  }
})

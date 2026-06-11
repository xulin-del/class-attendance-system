const { post } = require('../../../utils/api')

Page({
  data: {
    question: '',
    answer: '',
    loading: false,
    history: []
  },

  onLoad() {},

  onInputChange(e) {
    this.setData({ question: e.detail.value })
  },

  async submitQuestion() {
    const question = this.data.question.trim()
    if (!question) {
      wx.showToast({ title: '请输入问题', icon: 'none' })
      return
    }
    if (this.data.loading) return

    this.setData({ loading: true, answer: '' })

    try {
      const res = await post('/attendance/ai/query', { question })
      if (res.success) {
        this.setData({
          answer: res.data,
          history: [{ question, answer: res.data, time: this.formatTime() }, ...this.data.history].slice(0, 20)
        })
      } else {
        this.setData({ answer: '查询失败：' + res.message })
      }
    } catch (err) {
      console.error(err)
      this.setData({ answer: '网络错误，请重试' })
    } finally {
      this.setData({ loading: false })
    }
  },

  formatTime() {
    const now = new Date()
    const h = now.getHours().toString().padStart(2, '0')
    const m = now.getMinutes().toString().padStart(2, '0')
    return h + ':' + m
  },

  clearHistory() {
    this.setData({ history: [], answer: '', question: '' })
  },

  useSuggestion(e) {
    const q = e.currentTarget.dataset.question
    this.setData({ question: q })
    this.submitQuestion()
  }
})

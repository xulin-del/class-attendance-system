const { get } = require('../../../utils/api')

Page({
  data: {
    userInfo: {},
    overallStats: {},
    weeklyStats: [],
    chartWidth: 0,
    chartHeight: 200,
    ringSize: 160
  },

  onLoad() {
    const userInfo = wx.getStorageSync('userInfo')
    this.setData({ userInfo })
    this.initChartSize()
  },

  onShow() {
    this.loadOverallStats()
    this.loadWeeklyStats()
  },

  initChartSize() {
    const systemInfo = wx.getSystemInfoSync()
    const chartWidth = systemInfo.windowWidth - 60
    this.setData({ chartWidth })
  },

  async loadOverallStats() {
    try {
      const res = await get('/attendance/teacherStats', { teacherId: this.data.userInfo.teacherId })
      if (res.success) {
        this.setData({ overallStats: res.data })
        this.drawRing(res.data.rate || 0)
      }
    } catch (err) {
      console.error(err)
    }
  },

  async loadWeeklyStats() {
    try {
      const res = await get('/attendance/weeklyStats', { teacherId: this.data.userInfo.teacherId })
      if (res.success) {
        this.setData({ weeklyStats: res.data })
        this.drawBarChart(res.data)
      }
    } catch (err) {
      console.error(err)
    }
  },

  drawRing(rate) {
    const { ringSize } = this.data
    const query = wx.createSelectorQuery()
    query.select('#ringCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0]) return

        const canvas = res[0].node
        const ctx = canvas.getContext('2d')
        const dpr = wx.getSystemInfoSync().pixelRatio
        canvas.width = ringSize * dpr
        canvas.height = ringSize * dpr
        ctx.scale(dpr, dpr)

        const centerX = ringSize / 2
        const centerY = ringSize / 2
        const radius = (ringSize / 2) - 15
        const lineWidth = 12

        ctx.clearRect(0, 0, ringSize, ringSize)

        ctx.beginPath()
        ctx.arc(centerX, centerY, radius, 0, Math.PI * 2)
        ctx.strokeStyle = '#E8F5E9'
        ctx.lineWidth = lineWidth
        ctx.stroke()

        if (rate > 0) {
          const endAngle = (rate / 100) * Math.PI * 2 - Math.PI / 2
          const gradient = ctx.createLinearGradient(0, 0, ringSize, ringSize)
          gradient.addColorStop(0, '#43A047')
          gradient.addColorStop(1, '#2E7D32')

          ctx.beginPath()
          ctx.arc(centerX, centerY, radius, -Math.PI / 2, endAngle)
          ctx.strokeStyle = gradient
          ctx.lineWidth = lineWidth
          ctx.lineCap = 'round'
          ctx.stroke()
        }
      })
  },

  drawBarChart(weeklyStats) {
    const { chartWidth, chartHeight } = this.data
    if (!weeklyStats || weeklyStats.length === 0) return

    const query = wx.createSelectorQuery()
    query.select('#barCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0]) return

        const canvas = res[0].node
        const ctx = canvas.getContext('2d')
        const dpr = wx.getSystemInfoSync().pixelRatio
        canvas.width = chartWidth * dpr
        canvas.height = chartHeight * dpr
        ctx.scale(dpr, dpr)

        const width = chartWidth
        const height = chartHeight
        const padding = { top: 20, right: 15, bottom: 35, left: 35 }
        const chartW = width - padding.left - padding.right
        const chartH = height - padding.top - padding.bottom

        ctx.fillStyle = '#fff'
        ctx.fillRect(0, 0, width, height)

        const maxRate = 100
        const barWidth = chartW / weeklyStats.length * 0.5
        const barGap = chartW / weeklyStats.length

        ctx.strokeStyle = '#e8e8e8'
        ctx.lineWidth = 1
        for (let i = 0; i <= 4; i++) {
          const y = padding.top + chartH - (chartH / 4 * i)
          ctx.beginPath()
          ctx.moveTo(padding.left, y)
          ctx.lineTo(width - padding.right, y)
          ctx.stroke()

          ctx.fillStyle = '#999'
          ctx.font = '10px sans-serif'
          ctx.textAlign = 'right'
          ctx.fillText((i * 25) + '%', padding.left - 5, y + 3)
        }

        const dayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
        weeklyStats.forEach((stat, index) => {
          const x = padding.left + barGap * index + (barGap - barWidth) / 2
          const rate = stat.rate || 0
          const barHeight = Math.max((rate / maxRate) * chartH, 2)
          const barY = padding.top + chartH - barHeight

          if (rate > 0) {
            const gradient = ctx.createLinearGradient(x, barY, x, padding.top + chartH)
            gradient.addColorStop(0, '#43A047')
            gradient.addColorStop(1, '#2E7D32')
            ctx.fillStyle = gradient
          } else {
            ctx.fillStyle = '#E8F5E9'
          }

          const radius = 3
          ctx.beginPath()
          ctx.moveTo(x + radius, barY)
          ctx.lineTo(x + barWidth - radius, barY)
          ctx.quadraticCurveTo(x + barWidth, barY, x + barWidth, barY + radius)
          ctx.lineTo(x + barWidth, barY + barHeight)
          ctx.lineTo(x, barY + barHeight)
          ctx.lineTo(x, barY + radius)
          ctx.quadraticCurveTo(x, barY, x + radius, barY)
          ctx.closePath()
          ctx.fill()

          ctx.fillStyle = '#333'
          ctx.font = '11px sans-serif'
          ctx.textAlign = 'center'
          const dateStr = stat.date || ''
          const dayIndex = new Date(dateStr).getDay()
          const dayName = dayNames[dayIndex === 0 ? 6 : dayIndex - 1]
          ctx.fillText(dayName, x + barWidth / 2, height - padding.bottom + 18)

          if (rate > 0) {
            ctx.fillStyle = '#2E7D32'
            ctx.font = 'bold 10px sans-serif'
            ctx.fillText(Math.round(rate) + '%', x + barWidth / 2, barY - 6)
          }
        })
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
    if (index !== 3) {
      wx.redirectTo({ url: urls[index] })
    }
  }
})

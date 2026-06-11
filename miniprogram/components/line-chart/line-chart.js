Component({
  properties: {
    data: {
      type: Array,
      value: []
    },
    height: {
      type: Number,
      value: 200
    }
  },

  data: {
    canvasWidth: 0,
    canvasHeight: 0
  },

  observers: {
    'data': function(data) {
      if (data && data.length > 0) {
        this.drawChart()
      }
    }
  },

  ready() {
    this.getCanvasSize()
  },

  methods: {
    getCanvasSize() {
      const query = this.createSelectorQuery()
      query.select('#chartCanvas').boundingClientRect((rect) => {
        if (rect) {
          this.setData({
            canvasWidth: rect.width,
            canvasHeight: rect.height
          }, () => {
            if (this.properties.data && this.properties.data.length > 0) {
              this.drawChart()
            }
          })
        }
      }).exec()
    },

    drawChart() {
      const data = this.properties.data
      if (!data || data.length === 0) return

      const query = this.createSelectorQuery()
      query.select('#chartCanvas')
        .fields({ node: true, size: true })
        .exec((res) => {
          if (!res || !res[0] || !res[0].node) return

          const canvas = res[0].node
          const width = res[0].width
          const height = res[0].height
          const dpr = wx.getWindowInfo().pixelRatio

          canvas.width = width * dpr
          canvas.height = height * dpr

          const ctx = canvas.getContext('2d')
          ctx.scale(dpr, dpr)

          this.doDraw(ctx, width, height, data)
        })
    },

    doDraw(ctx, width, height, data) {
      const padding = { top: 30, right: 20, bottom: 40, left: 50 }
      const chartWidth = width - padding.left - padding.right
      const chartHeight = height - padding.top - padding.bottom

      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, width, height)

      const rates = data.map(d => d.rate)
      const maxRate = Math.max(...rates, 100)
      const minRate = 0

      ctx.strokeStyle = '#edf0eb'
      ctx.lineWidth = 1
      for (let i = 0; i <= 4; i++) {
        const y = padding.top + (chartHeight / 4) * i
        ctx.beginPath()
        ctx.moveTo(padding.left, y)
        ctx.lineTo(width - padding.right, y)
        ctx.stroke()

        const value = Math.round(maxRate - (maxRate / 4) * i)
        ctx.fillStyle = '#7a867d'
        ctx.font = '10px sans-serif'
        ctx.textAlign = 'right'
        ctx.fillText(value + '%', padding.left - 8, y + 3)
      }

      const points = []
      const stepX = chartWidth / (data.length - 1)

      data.forEach((item, index) => {
        const x = padding.left + stepX * index
        const y = padding.top + chartHeight - (item.rate / maxRate) * chartHeight
        points.push({ x, y, date: item.date, rate: item.rate })
      })

      ctx.beginPath()
      ctx.strokeStyle = '#2f7d4a'
      ctx.lineWidth = 2
      points.forEach((point, index) => {
        if (index === 0) {
          ctx.moveTo(point.x, point.y)
        } else {
          ctx.lineTo(point.x, point.y)
        }
      })
      ctx.stroke()

      ctx.beginPath()
      ctx.fillStyle = 'rgba(47, 125, 74, 0.10)'
      ctx.moveTo(points[0].x, padding.top + chartHeight)
      points.forEach((point) => {
        ctx.lineTo(point.x, point.y)
      })
      ctx.lineTo(points[points.length - 1].x, padding.top + chartHeight)
      ctx.closePath()
      ctx.fill()

      points.forEach((point) => {
        ctx.beginPath()
        ctx.fillStyle = '#2f7d4a'
        ctx.arc(point.x, point.y, 4, 0, Math.PI * 2)
        ctx.fill()

        ctx.beginPath()
        ctx.fillStyle = '#ffffff'
        ctx.arc(point.x, point.y, 2, 0, Math.PI * 2)
        ctx.fill()
      })

      ctx.fillStyle = '#526158'
      ctx.font = '10px sans-serif'
      ctx.textAlign = 'center'
      points.forEach((point, index) => {
        const dateStr = point.date.substring(5)
        ctx.fillText(dateStr, point.x, height - padding.bottom + 15)
      })
    }
  }
})

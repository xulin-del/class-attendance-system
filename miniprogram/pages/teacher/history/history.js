const { get } = require('../../../utils/api')

function formatDate(date) {
  const year = date.getFullYear()
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  return `${year}-${month}-${day}`
}

Page({
  data: {
    selectedDate: '',
    records: [],
    groupedRecords: [],
    signedCount: 0,
    absentCount: 0,
    unsignedCount: 0
  },

  onLoad() {
    this.setData({ selectedDate: formatDate(new Date()) })
  },

  onShow() {
    this.loadRecords()
  },

  showDatePicker() {
    this.setData({ showPicker: true })
  },

  onDateChange(e) {
    this.setData({ selectedDate: e.detail.value })
    this.loadRecords()
  },

  async loadRecords() {
    const userInfo = wx.getStorageSync('userInfo')
    try {
      const res = await get('/attendance/historyByDate', {
        teacherId: userInfo.teacherId,
        date: this.data.selectedDate
      })
      if (res.success) {
        const records = res.data
        let signedCount = 0, absentCount = 0, unsignedCount = 0
        records.forEach(r => {
          if (r.status === '已签到') signedCount++
          else if (r.status === '缺勤') absentCount++
          else unsignedCount++
        })
        const groupedRecords = this.groupByCourse(records)
        this.setData({
          records,
          groupedRecords,
          signedCount,
          absentCount,
          unsignedCount
        })
      }
    } catch (err) {
      console.error(err)
    }
  },

  groupByCourse(records) {
    const map = {}
    records.forEach(r => {
      const key = r.courseName
      if (!map[key]) {
        map[key] = {
          courseName: r.courseName,
          className: r.className,
          records: []
        }
      }
      map[key].records.push(r)
    })
    return Object.values(map)
  }
})

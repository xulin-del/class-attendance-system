const TIMEOUT = 15000

// 本地开发环境
// const BASE_URL = 'http://localhost:8081/api'
// 局域网测试环境（改成你的电脑IP）
// const BASE_URL = 'http://192.168.x.x:8081/api'
// 生产环境（部署到云服务器后修改）
const BASE_URL = 'http://localhost:8081/api'

function getStatusClass(status) {
  const statusMap = {
    '已签到': 'status-signed',
    '未签到': 'status-unsigned',
    '缺勤': 'status-absent'
  }
  return statusMap[status] || 'status-unsigned'
}

function request(options) {
  return new Promise((resolve, reject) => {
    const controller = {
      aborted: false,
      abort() {
        this.aborted = true
      }
    }
    
    const timer = setTimeout(() => {
      controller.abort()
      reject(new Error('request timeout'))
      wx.hideLoading()
      wx.showToast({
        title: 'network timeout',
        icon: 'none',
        duration: 2000
      })
    }, options.timeout || TIMEOUT)
    
    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'content-type': 'application/json'
      },
      timeout: options.timeout || TIMEOUT,
      success(res) {
        clearTimeout(timer)
        if (controller.aborted) return
        if (res.statusCode === 200) {
          resolve(res.data)
        } else {
          reject(new Error('server error: ' + res.statusCode))
        }
      },
      fail(err) {
        clearTimeout(timer)
        if (controller.aborted) return
        console.error('request failed:', err)
        reject(err)
        wx.hideLoading()
        wx.showToast({
          title: 'network error',
          icon: 'none',
          duration: 2000
        })
      }
    })
  })
}

function get(url, data, options = {}) {
  return request({
    url,
    method: 'GET',
    data,
    ...options
  })
}

function post(url, data, options = {}) {
  return request({
    url,
    method: 'POST',
    data,
    ...options
  })
}

module.exports = {
  request,
  get,
  post,
  getStatusClass,
  TIMEOUT,
  BASE_URL
}
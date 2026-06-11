const app = getApp()

const TIMEOUT = 15000

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
      url: app.globalData.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: options.header || {
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

function get(url, data) {
  return request({ url, method: 'GET', data })
}

function post(url, data) {
  return request({ url, method: 'POST', data })
}

module.exports = {
  request,
  get,
  post,
  TIMEOUT
}
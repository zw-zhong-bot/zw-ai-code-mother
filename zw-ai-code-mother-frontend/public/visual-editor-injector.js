/**
 * 可视化编辑器注入脚本（浏览器可直接执行版）
 * 功能：在 iframe 页面中启用元素悬停高亮与点击选中，并通过 postMessage 与父页面通信
 * 主要属性：
 * - isEnabled: 是否启用编辑模式
 * - selectedElements: 已选中的元素集合
 * - hoveredElement: 当前悬浮元素
 * 主要方法：
 * - enableEditMode/disableEditMode：切换编辑模式
 * - highlightElement/removeHighlight：高亮/移除高亮
 * - selectElement/deselectElement：选择/取消选择元素
 * - findElementByXPath：根据 XPath 定位元素
 * - sendMessageToParent：向父窗口发送消息
 * 作者：ZW
 * 版本：v1.0.0
 * 创建时间：2025-09-19
 * 修改时间：2025-09-19
 */

/**
 * 生成元素的XPath路径
 * @param {Element} element DOM元素
 * @returns {string} XPath路径
 * @author ZW
 */
function __ve_getElementXPath(element) {
  if (element.id) {
    return '//*[@id="' + element.id + '"]'
  }
  if (element === document.body) {
    return '/html/body'
  }
  var ix = 0
  var siblings = (element.parentNode && element.parentNode.children) || []
  for (var i = 0; i < siblings.length; i++) {
    var sibling = siblings[i]
    if (sibling === element) {
      var tagName = element.tagName.toLowerCase()
      var parentPath = element.parentElement ? __ve_getElementXPath(element.parentElement) : ''
      return parentPath + '/' + tagName + '[' + (ix + 1) + ']'
    }
    if (sibling.tagName === element.tagName) {
      ix++
    }
  }
  return ''
}

/**
 * 获取元素属性
 * @param {Element} element DOM元素
 * @returns {Record<string,string>} 属性对象
 * @author ZW
 */
function __ve_getElementAttributes(element) {
  var attributes = {}
  for (var i = 0; i < element.attributes.length; i++) {
    var attr = element.attributes[i]
    attributes[attr.name] = attr.value
  }
  return attributes
}

/**
 * 创建高亮样式
 * @returns {void}
 * @author ZW
 */
function __ve_createHighlightStyles() {
  var styleId = 'visual-editor-styles'
  if (document.getElementById(styleId)) return

  var style = document.createElement('style')
  style.id = styleId
  // 使用 CSS 变量便于父页未来动态主题
  style.textContent =
    ':root{--ve-hover:#1890ff;--ve-selected:#52c41a;--ve-hover-outline:2px;--ve-selected-outline:3px}' +
    '.visual-editor-hover{outline:var(--ve-hover-outline) dashed var(--ve-hover) !important;outline-offset:2px !important;cursor:pointer !important;position:relative !important;}' +
    '.visual-editor-selected{outline:var(--ve-selected-outline) solid var(--ve-selected) !important;outline-offset:2px !important;position:relative !important;}' +
    '.visual-editor-hover::before{content:attr(data-visual-editor-info);position:absolute;top:-25px;left:0;background:var(--ve-hover);color:#fff;padding:2px 6px;font-size:12px;border-radius:3px;white-space:nowrap;z-index:10000;pointer-events:none;}' +
    '.visual-editor-selected::before{content:attr(data-visual-editor-info);position:absolute;top:-25px;left:0;background:var(--ve-selected);color:#fff;padding:2px 6px;font-size:12px;border-radius:3px;white-space:nowrap;z-index:10000;pointer-events:none;}'
  document.head.appendChild(style)
}

/**
 * VisualEditor 类
 * @class
 * @classdesc 提供 iframe 内的元素选择与高亮，负责与父页面通信
 * @property {boolean} isEnabled 是否启用编辑模式
 * @property {Set<Element>} selectedElements 已选元素集合
 * @property {Element|null} hoveredElement 当前悬浮元素
 * 作者：ZW
 */
var __ve_port = null
var __ve_lastHoverEl = null
var __ve_hoverTimer = null
function VisualEditor() {
  this.isEnabled = false
  this.selectedElements = new Set()
  this.hoveredElement = null

  this.handleMouseOver = this.handleMouseOver.bind(this)
  this.handleMouseOut = this.handleMouseOut.bind(this)
  this.handleClick = this.handleClick.bind(this)

  this.init()
}

/**
 * 初始化
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.init = function () {
  __ve_createHighlightStyles()
  this.setupMessageListener()
  this.notifyParentReady()
}

/**
 * 设置消息监听器
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.setupMessageListener = function () {
  window.addEventListener('message', (event) => {
    // 初始化 MessageChannel：父页传入 port2
    if (event.data && event.data.type === 'init-port' && event.ports && event.ports[0]) {
      __ve_port = event.ports[0]
      var self = this
      __ve_port.onmessage = function (e) {
        var ed = e.data
        if (!ed || ed.source !== 'visual-editor') return
        var type = ed.type
        var data = ed.data
        if (type === 'enable-edit-mode') self.enableEditMode()
        else if (type === 'disable-edit-mode') self.disableEditMode()
        else if (type === 'element-selected') self.selectElement(data.xpath, data.elementId)
        else if (type === 'element-deselected') self.deselectElement(data.xpath)
        else if (type === 'edit-text') self.applyTextEdit(ed)
        else if (type === 'edit-attribute') self.applyAttributeEdit(ed)
        else if (type === 'apply-op') self.applyOp(ed)
      }
      return
    }

    // 兼容已有 postMessage 通信
    if (!event.data || event.data.source !== 'visual-editor') return
    var type = event.data.type
    var data = event.data.data
    if (type === 'enable-edit-mode') this.enableEditMode()
    else if (type === 'disable-edit-mode') this.disableEditMode()
    else if (type === 'element-selected') this.selectElement(data.xpath, data.elementId)
    else if (type === 'element-deselected') this.deselectElement(data.xpath)
    else if (type === 'edit-text') this.applyTextEdit(event.data)
    else if (type === 'edit-attribute') this.applyAttributeEdit(event.data)
    else if (type === 'apply-op') this.applyOp(event.data)
  })
}

/**
 * 通知父窗口 iframe 就绪
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.notifyParentReady = function () {
  if (window.parent && window.parent !== window) {
    window.parent.postMessage(
      {
        type: 'iframe-ready',
        source: 'visual-editor-iframe',
      },
      '*',
    )
  }
}

/**
 * 应用文本编辑（作者：ZW）
 * 功能：根据父页下发的 edit-text 指令设置元素文本
 * 参数：ed { type, source, opId, xpath, text, kind }
 * 返回值：void
 * 异常：无（错误通过 console.error 打印）
 */
VisualEditor.prototype.applyTextEdit = function (ed) {
  try {
    var xpath = ed.xpath
    var text = String(ed.text || '')
    var el = this.findElementByXPath(xpath)
    if (!el) return
    var prev = (el.textContent || '').toString()
    el.textContent = text
    this.sendMessageToParent('op-applied', {
      source: 'visual-editor-iframe',
      opId: String(ed.opId || Date.now()),
      kind: 'edit-text',
      xpath: xpath,
      name: null,
      prevValue: prev,
      newValue: text,
    })
  } catch (e) {
    console.error('applyTextEdit error:', e)
  }
}

/**
 * 应用属性编辑（作者：ZW）
 * 功能：根据父页下发的 edit-attribute 指令设置元素属性
 * 参数：ed { type, source, opId, xpath, name, value, kind }
 * 返回值：void
 * 异常：无
 */
VisualEditor.prototype.applyAttributeEdit = function (ed) {
  try {
    var xpath = ed.xpath
    var name = String(ed.name || '')
    var value = String(ed.value || '')
    if (!name) return
    var el = this.findElementByXPath(xpath)
    if (!el) return
    var prev = el.getAttribute(name)
    if (value === '') el.removeAttribute(name)
    else el.setAttribute(name, value)
    this.sendMessageToParent('op-applied', {
      source: 'visual-editor-iframe',
      opId: String(ed.opId || Date.now()),
      kind: 'edit-attribute',
      xpath: xpath,
      name: name,
      prevValue: prev == null ? '' : String(prev),
      newValue: value,
    })
  } catch (e) {
    console.error('applyAttributeEdit error:', e)
  }
}

/**
 * 应用撤销/重做（作者：ZW）
 * 功能：根据父页下发的 apply-op 指令回放文本或属性修改
 * 参数：ed { type, source, xpath, kind, text | (name,value) }
 * 返回值：void
 * 异常：无
 */
VisualEditor.prototype.applyOp = function (ed) {
  try {
    var xpath = ed.xpath
    var kind = ed.kind
    var el = this.findElementByXPath(xpath)
    if (!el) return
    if (kind === 'edit-text') {
      var text = String(ed.text || '')
      el.textContent = text
      this.sendMessageToParent('op-applied', {
        source: 'visual-editor-iframe',
        opId: String(ed.opId || Date.now()),
        kind: 'edit-text',
        xpath: xpath,
        name: null,
        prevValue: null,
        newValue: text,
      })
    } else if (kind === 'edit-attribute') {
      var name = String(ed.name || '')
      var value = String(ed.value || '')
      if (!name) return
      if (value === '') el.removeAttribute(name)
      else el.setAttribute(name, value)
      this.sendMessageToParent('op-applied', {
        source: 'visual-editor-iframe',
        opId: String(ed.opId || Date.now()),
        kind: 'edit-attribute',
        xpath: xpath,
        name: name,
        prevValue: null,
        newValue: value,
      })
    }
  } catch (e) {
    console.error('applyOp error:', e)
  }
}

/**
 * 启用编辑模式
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.enableEditMode = function () {
  if (this.isEnabled) return
  this.isEnabled = true
  document.addEventListener('mouseover', this.handleMouseOver)
  document.addEventListener('mouseout', this.handleMouseOut)
  document.addEventListener('click', this.handleClick)
  this.addEditModeIndicator()
}

/**
 * 禁用编辑模式
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.disableEditMode = function () {
  if (!this.isEnabled) return
  this.isEnabled = false
  document.removeEventListener('mouseover', this.handleMouseOver)
  document.removeEventListener('mouseout', this.handleMouseOut)
  document.removeEventListener('click', this.handleClick)
  this.clearAllHighlights()
  this.removeEditModeIndicator()
}

/**
 * 添加模式指示器
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.addEditModeIndicator = function () {
  var indicator = document.createElement('div')
  indicator.id = 'visual-editor-indicator'
  indicator.style.cssText =
    'position:fixed;top:10px;right:10px;background:#1890ff;color:#fff;padding:8px 12px;border-radius:4px;font-size:12px;z-index:10001;pointer-events:none;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;'
  indicator.textContent = '🎨 可视化编辑模式'
  document.body.appendChild(indicator)
}

/**
 * 移除模式指示器
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.removeEditModeIndicator = function () {
  var indicator = document.getElementById('visual-editor-indicator')
  if (indicator) indicator.remove()
}

/**
 * 处理鼠标悬浮
 * @param {MouseEvent} event 事件对象
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.handleMouseOver = function (event) {
  if (!this.isEnabled) return
  var target = event.target
  if (!target || target === document.body || target === document.documentElement) return
  if (
    target.id === 'visual-editor-indicator' ||
    (target.closest && target.closest('#visual-editor-indicator'))
  )
    return
  // 高亮立即切换
  this.highlightElement(target, 'hover')

  // 仅在目标变化时才发送 hover，加入节流
  if (target === __ve_lastHoverEl) return
  __ve_lastHoverEl = target
  if (__ve_hoverTimer) {
    clearTimeout(__ve_hoverTimer)
    __ve_hoverTimer = null
  }
  var self = this
  __ve_hoverTimer = setTimeout(function () {
    self.sendMessageToParent('element-hover', { elementId: self.getElementId(target) })
  }, 60)
}

/**
 * 处理鼠标离开
 * @param {MouseEvent} event 事件对象
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.handleMouseOut = function (event) {
  if (!this.isEnabled) return
  var target = event.target
  if (target && !this.selectedElements.has(target)) {
    this.removeHighlight(target, 'hover')
  }
  if (this.hoveredElement === target) {
    this.hoveredElement = null
    // 清理 lastHover 与节流计时器
    if (__ve_lastHoverEl === target) __ve_lastHoverEl = null
    if (__ve_hoverTimer) {
      clearTimeout(__ve_hoverTimer)
      __ve_hoverTimer = null
    }
    this.sendMessageToParent('element-hover-end')
  }
}

/**
 * 处理点击
 * @param {MouseEvent} event 事件对象
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.handleClick = function (event) {
  if (!this.isEnabled) return
  event.preventDefault()
  event.stopPropagation()
  var target = event.target
  if (!target || target === document.body || target === document.documentElement) return
  if (
    target.id === 'visual-editor-indicator' ||
    (target.closest && target.closest('#visual-editor-indicator'))
  )
    return

  var elementData = {
    tagName: target.tagName.toLowerCase(),
    className: target.className,
    textContent: (target.textContent || '').trim(),
    xpath: __ve_getElementXPath(target),
    attributes: __ve_getElementAttributes(target),
    // 携带修饰键，便于父页决定多选/替换选中策略
    metaKey: !!event.metaKey,
    ctrlKey: !!event.ctrlKey,
    shiftKey: !!event.shiftKey,
  }
  this.sendMessageToParent('element-click', elementData)
}

/**
 * 选择元素
 * @param {string} xpath 元素 XPath
 * @param {string} elementId 元素ID
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.selectElement = function (xpath, elementId) {
  var element = this.findElementByXPath(xpath)
  if (element) {
    this.selectedElements.add(element)
    this.highlightElement(element, 'selected')
  }
}

/**
 * 取消选择元素
 * @param {string} xpath 元素 XPath
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.deselectElement = function (xpath) {
  var element = this.findElementByXPath(xpath)
  if (element) {
    this.selectedElements.delete(element)
    this.removeHighlight(element, 'selected')
  }
}

/**
 * 高亮元素
 * @param {Element} element 目标元素
 * @param {'hover'|'selected'} type 类型
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.highlightElement = function (element, type) {
  var className = type === 'hover' ? 'visual-editor-hover' : 'visual-editor-selected'
  var info =
    element.tagName.toLowerCase() +
    (element.className ? '.' + String(element.className).split(' ')[0] : '')
  element.classList.add(className)
  element.setAttribute('data-visual-editor-info', info)
}

/**
 * 移除高亮
 * @param {Element} element 目标元素
 * @param {'hover'|'selected'} type 类型
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.removeHighlight = function (element, type) {
  var className = type === 'hover' ? 'visual-editor-hover' : 'visual-editor-selected'
  element.classList.remove(className)
  if (
    !element.classList.contains('visual-editor-hover') &&
    !element.classList.contains('visual-editor-selected')
  ) {
    element.removeAttribute('data-visual-editor-info')
  }
}

/**
 * 清空全部高亮
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.clearAllHighlights = function () {
  var highlighted = document.querySelectorAll('.visual-editor-hover, .visual-editor-selected')
  highlighted.forEach(function (el) {
    el.classList.remove('visual-editor-hover', 'visual-editor-selected')
    el.removeAttribute('data-visual-editor-info')
  })
  this.selectedElements.clear()
}

/**
 * 根据 XPath 查找元素
 * @param {string} xpath XPath
 * @returns {Element|null}
 * @author ZW
 */
VisualEditor.prototype.findElementByXPath = function (xpath) {
  try {
    var result = document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
    return result.singleNodeValue
  } catch (e) {
    console.error('Error finding element by XPath:', e)
    return null
  }
}

/**
 * 生成元素ID
 * @param {Element} element DOM元素
 * @returns {string} 元素ID
 * @author ZW
 */
VisualEditor.prototype.getElementId = function (element) {
  return element.tagName.toLowerCase() + '_' + __ve_getElementXPath(element) + '_' + Date.now()
}

/**
 * 发送消息到父窗口
 * @param {string} type 类型
 * @param {any} data 数据
 * @returns {void}
 * @author ZW
 */
VisualEditor.prototype.sendMessageToParent = function (type, data) {
  // 优先使用 MessageChannel，降低全局 postMessage 分发开销
  if (__ve_port) {
    __ve_port.postMessage({
      type: type,
      data: data,
      source: 'visual-editor-iframe',
    })
    return
  }
  if (window.parent && window.parent !== window) {
    window.parent.postMessage(
      {
        type: type,
        data: data,
        source: 'visual-editor-iframe',
      },
      '*',
    )
  }
}

/**
 * 初始化入口（仅在 iframe 中自动执行）
 * @returns {void}
 * @author ZW
 */
function initVisualEditor() {
  if (!window.__visualEditorInstance) {
    window.__visualEditorInstance = new VisualEditor()
  }
}

if (window.parent && window.parent !== window) {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initVisualEditor)
  } else {
    initVisualEditor()
  }
}

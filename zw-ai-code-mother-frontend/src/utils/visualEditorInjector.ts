/**
 * 可视化编辑器注入脚本
 * 用于在iframe中注入元素选择和高亮功能
 * @author ZW
 */

/**
 * 生成元素的XPath路径
 * @param element DOM元素
 * @returns XPath路径
 */
function getElementXPath(element: Element): string {
  if (element.id) {
    return `//*[@id="${element.id}"]`
  }

  if (element === document.body) {
    return '/html/body'
  }

  let ix = 0
  const siblings = element.parentNode?.children || []

  for (let i = 0; i < siblings.length; i++) {
    const sibling = siblings[i]
    if (sibling === element) {
      const tagName = element.tagName.toLowerCase()
      const parentPath = element.parentElement ? getElementXPath(element.parentElement) : ''
      return `${parentPath}/${tagName}[${ix + 1}]`
    }
    if (sibling.tagName === element.tagName) {
      ix++
    }
  }

  return ''
}

/**
 * 获取元素的所有属性
 * @param element DOM元素
 * @returns 属性对象
 */
function getElementAttributes(element: Element): Record<string, string> {
  const attributes: Record<string, string> = {}
  for (let i = 0; i < element.attributes.length; i++) {
    const attr = element.attributes[i]
    attributes[attr.name] = attr.value
  }
  return attributes
}

/**
 * 创建高亮样式
 */
function createHighlightStyles(): void {
  const styleId = 'visual-editor-styles'
  if (document.getElementById(styleId)) {
    return
  }

  const style = document.createElement('style')
  style.id = styleId
  style.textContent = `
    .visual-editor-hover {
      outline: 2px dashed #1890ff !important;
      outline-offset: 2px !important;
      cursor: pointer !important;
      position: relative !important;
    }

    .visual-editor-selected {
      outline: 3px solid #52c41a !important;
      outline-offset: 2px !important;
      position: relative !important;
    }

    .visual-editor-hover::before {
      content: attr(data-visual-editor-info);
      position: absolute;
      top: -25px;
      left: 0;
      background: #1890ff;
      color: white;
      padding: 2px 6px;
      font-size: 12px;
      border-radius: 3px;
      white-space: nowrap;
      z-index: 10000;
      pointer-events: none;
    }

    .visual-editor-selected::before {
      content: attr(data-visual-editor-info);
      position: absolute;
      top: -25px;
      left: 0;
      background: #52c41a;
      color: white;
      padding: 2px 6px;
      font-size: 12px;
      border-radius: 3px;
      white-space: nowrap;
      z-index: 10000;
      pointer-events: none;
    }
  `
  document.head.appendChild(style)
}

/**
 * 可视化编辑器类
 */
class VisualEditor {
  private isEnabled = false
  private selectedElements = new Set<Element>()
  private hoveredElement: Element | null = null

  constructor() {
    this.init()
  }

  /**
   * 初始化编辑器
   */
  private init(): void {
    createHighlightStyles()
    this.setupMessageListener()
    this.notifyParentReady()
  }

  /**
   * 设置消息监听器
   */
  private setupMessageListener(): void {
    window.addEventListener('message', (event) => {
      if (!event.data || event.data.source !== 'visual-editor') {
        return
      }

      const { type, data } = event.data

      switch (type) {
        case 'enable-edit-mode':
          this.enableEditMode()
          break
        case 'disable-edit-mode':
          this.disableEditMode()
          break
        case 'element-selected':
          this.selectElement(data.xpath, data.elementId)
          break
        case 'element-deselected':
          this.deselectElement(data.xpath)
          break
        default:
          console.warn('Unknown message type:', type)
      }
    })
  }

  /**
   * 通知父窗口iframe已准备就绪
   */
  private notifyParentReady(): void {
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
   * 启用编辑模式
   */
  private enableEditMode(): void {
    if (this.isEnabled) return

    this.isEnabled = true
    document.addEventListener('mouseover', this.handleMouseOver)
    document.addEventListener('mouseout', this.handleMouseOut)
    document.addEventListener('click', this.handleClick)

    // 添加编辑模式指示器
    this.addEditModeIndicator()
  }

  /**
   * 禁用编辑模式
   */
  private disableEditMode(): void {
    if (!this.isEnabled) return

    this.isEnabled = false
    document.removeEventListener('mouseover', this.handleMouseOver)
    document.removeEventListener('mouseout', this.handleMouseOut)
    document.removeEventListener('click', this.handleClick)

    // 清除所有高亮
    this.clearAllHighlights()
    this.removeEditModeIndicator()
  }

  /**
   * 添加编辑模式指示器
   */
  private addEditModeIndicator(): void {
    const indicator = document.createElement('div')
    indicator.id = 'visual-editor-indicator'
    indicator.style.cssText = `
      position: fixed;
      top: 10px;
      right: 10px;
      background: #1890ff;
      color: white;
      padding: 8px 12px;
      border-radius: 4px;
      font-size: 12px;
      z-index: 10001;
      pointer-events: none;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    `
    indicator.textContent = '🎨 可视化编辑模式'
    document.body.appendChild(indicator)
  }

  /**
   * 移除编辑模式指示器
   */
  private removeEditModeIndicator(): void {
    const indicator = document.getElementById('visual-editor-indicator')
    if (indicator) {
      indicator.remove()
    }
  }

  /**
   * 处理鼠标悬浮
   */
  private handleMouseOver = (event: MouseEvent): void => {
    if (!this.isEnabled) return

    const target = event.target as Element
    if (!target || target === document.body || target === document.documentElement) {
      return
    }

    // 避免选择编辑器相关元素
    if (target.id === 'visual-editor-indicator' || target.closest('#visual-editor-indicator')) {
      return
    }

    this.hoveredElement = target
    this.highlightElement(target, 'hover')

    // 通知父窗口
    this.sendMessageToParent('element-hover', {
      elementId: this.getElementId(target),
    })
  }

  /**
   * 处理鼠标离开
   */
  private handleMouseOut = (event: MouseEvent): void => {
    if (!this.isEnabled) return

    const target = event.target as Element
    if (target && !this.selectedElements.has(target)) {
      this.removeHighlight(target, 'hover')
    }

    if (this.hoveredElement === target) {
      this.hoveredElement = null
      this.sendMessageToParent('element-hover-end')
    }
  }

  /**
   * 处理点击事件
   */
  private handleClick = (event: MouseEvent): void => {
    if (!this.isEnabled) return

    event.preventDefault()
    event.stopPropagation()

    const target = event.target as Element
    if (!target || target === document.body || target === document.documentElement) {
      return
    }

    // 避免选择编辑器相关元素
    if (target.id === 'visual-editor-indicator' || target.closest('#visual-editor-indicator')) {
      return
    }

    const elementData = {
      tagName: target.tagName.toLowerCase(),
      className: target.className,
      textContent: target.textContent?.trim(),
      xpath: getElementXPath(target),
      attributes: getElementAttributes(target),
    }

    // 通知父窗口
    this.sendMessageToParent('element-click', elementData)
  }

  /**
   * 选择元素
   */
  private selectElement(xpath: string, elementId: string): void {
    const element = this.findElementByXPath(xpath)
    if (element) {
      this.selectedElements.add(element)
      this.highlightElement(element, 'selected')
    }
  }

  /**
   * 取消选择元素
   */
  private deselectElement(xpath: string): void {
    const element = this.findElementByXPath(xpath)
    if (element) {
      this.selectedElements.delete(element)
      this.removeHighlight(element, 'selected')
    }
  }

  /**
   * 高亮元素
   */
  private highlightElement(element: Element, type: 'hover' | 'selected'): void {
    const className = type === 'hover' ? 'visual-editor-hover' : 'visual-editor-selected'
    const info = `${element.tagName.toLowerCase()}${element.className ? '.' + element.className.split(' ')[0] : ''}`

    element.classList.add(className)
    element.setAttribute('data-visual-editor-info', info)
  }

  /**
   * 移除高亮
   */
  private removeHighlight(element: Element, type: 'hover' | 'selected'): void {
    const className = type === 'hover' ? 'visual-editor-hover' : 'visual-editor-selected'
    element.classList.remove(className)

    if (
      !element.classList.contains('visual-editor-hover') &&
      !element.classList.contains('visual-editor-selected')
    ) {
      element.removeAttribute('data-visual-editor-info')
    }
  }

  /**
   * 清除所有高亮
   */
  private clearAllHighlights(): void {
    const highlightedElements = document.querySelectorAll(
      '.visual-editor-hover, .visual-editor-selected',
    )
    highlightedElements.forEach((element) => {
      element.classList.remove('visual-editor-hover', 'visual-editor-selected')
      element.removeAttribute('data-visual-editor-info')
    })
    this.selectedElements.clear()
  }

  /**
   * 根据XPath查找元素
   */
  private findElementByXPath(xpath: string): Element | null {
    try {
      const result = document.evaluate(
        xpath,
        document,
        null,
        XPathResult.FIRST_ORDERED_NODE_TYPE,
        null,
      )
      return result.singleNodeValue as Element
    } catch (error) {
      console.error('Error finding element by XPath:', error)
      return null
    }
  }

  /**
   * 获取元素ID
   */
  private getElementId(element: Element): string {
    return `${element.tagName.toLowerCase()}_${getElementXPath(element)}_${Date.now()}`
  }

  /**
   * 向父窗口发送消息
   */
  private sendMessageToParent(type: string, data?: any): void {
    if (window.parent && window.parent !== window) {
      window.parent.postMessage(
        {
          type,
          data,
          source: 'visual-editor-iframe',
        },
        '*',
      )
    }
  }
}

// 创建可视化编辑器实例
export function initVisualEditor(): void {
  // 确保只初始化一次
  if (!(window as any).__visualEditor) {
    ;(window as any).__visualEditor = new VisualEditor()
  }
}

// 自动初始化（如果在iframe中）
if (window.parent && window.parent !== window) {
  // 等待DOM加载完成
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initVisualEditor)
  } else {
    initVisualEditor()
  }
}

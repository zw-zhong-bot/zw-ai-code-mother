import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'

/**
 * 选中的元素信息接口
 * @author ZW
 */
export interface SelectedElement {
  id: string
  tagName: string
  className: string
  textContent: string
  xpath: string
  selector: string
  attributes: Record<string, string>
}

/**
 * 可视化编辑器 Composable
 * 提供网站可视化编辑功能，包括元素选择、高亮显示、iframe通信等
 * @author ZW
 */
export function useVisualEditor() {
  // 编辑模式状态
  const isEditMode = ref(false)

  // 选中的元素列表
  const selectedElements = reactive<SelectedElement[]>([])

  // iframe 引用
  const iframeRef = ref<HTMLIFrameElement | null>(null)

  // 当前悬浮的元素
  const hoveredElement = ref<string | null>(null)

  // 当前选中的元素
  const activeElement = ref<string | null>(null)

  /**
   * 生成元素的唯一ID
   * @param element 元素信息
   * @returns 唯一ID
   */
  const generateElementId = (element: Partial<SelectedElement>): string => {
    return `${element.tagName}_${element.xpath}_${Date.now()}`
  }

  /**
   * 生成元素的CSS选择器
   * @param xpath XPath路径
   * @returns CSS选择器
   */
  const generateCSSSelector = (xpath: string): string => {
    // 简化的XPath到CSS选择器转换
    return xpath
      .replace(/\/html\[1\]/, 'html')
      .replace(/\/body\[1\]/, 'body')
      .replace(/\[(\d+)\]/g, ':nth-child($1)')
      .replace(/\//g, ' > ')
  }

  /**
   * 向iframe发送消息
   * @param type 消息类型
   * @param data 消息数据
   */
  const sendMessageToIframe = (type: string, data?: any) => {
    if (!iframeRef.value?.contentWindow) {
      console.warn('Iframe not ready for communication')
      return
    }

    try {
      iframeRef.value.contentWindow.postMessage(
        {
          type,
          data,
          source: 'visual-editor',
        },
        '*',
      )
    } catch (error) {
      console.error('Failed to send message to iframe:', error)
    }
  }

  /**
   * 处理来自iframe的消息
   * @param event 消息事件
   */
  const handleIframeMessage = (event: MessageEvent) => {
    // 验证消息来源
    if (!event.data || event.data.source !== 'visual-editor-iframe') {
      return
    }

    const { type, data } = event.data

    switch (type) {
      case 'element-hover':
        hoveredElement.value = data.elementId
        break

      case 'element-click':
        handleElementClick(data)
        break

      case 'element-hover-end':
        hoveredElement.value = null
        break

      case 'iframe-ready':
        console.log('Iframe is ready for visual editing')
        if (isEditMode.value) {
          sendMessageToIframe('enable-edit-mode')
        }
        break

      default:
        console.warn('Unknown message type from iframe:', type)
    }
  }

  /**
   * 处理元素点击
   * @param elementData 元素数据
   */
  const handleElementClick = (elementData: any) => {
    const elementInfo: SelectedElement = {
      id: generateElementId(elementData),
      tagName: elementData.tagName,
      className: elementData.className || '',
      textContent: elementData.textContent?.substring(0, 100) || '', // 限制文本长度
      xpath: elementData.xpath,
      selector: generateCSSSelector(elementData.xpath),
      attributes: elementData.attributes || {},
    }

    // 检查是否已经选中该元素
    const existingIndex = selectedElements.findIndex((el) => el.xpath === elementInfo.xpath)

    if (existingIndex === -1) {
      // 添加新选中的元素
      selectedElements.push(elementInfo)
      activeElement.value = elementInfo.id
      message.success(`已选中元素: ${elementInfo.tagName}`)
    } else {
      // 如果已存在，更新选中状态
      activeElement.value = selectedElements[existingIndex].id
      message.info('该元素已被选中')
    }

    // 通知iframe更新选中状态
    sendMessageToIframe('element-selected', {
      xpath: elementInfo.xpath,
      elementId: elementInfo.id,
    })
  }

  /**
   * 移除选中的元素
   * @param elementId 元素ID
   */
  const removeSelectedElement = (elementId: string) => {
    const index = selectedElements.findIndex((el) => el.id === elementId)
    if (index !== -1) {
      const element = selectedElements[index]
      selectedElements.splice(index, 1)

      // 通知iframe移除选中状态
      sendMessageToIframe('element-deselected', {
        xpath: element.xpath,
        elementId: elementId,
      })

      // 如果移除的是当前激活元素，清除激活状态
      if (activeElement.value === elementId) {
        activeElement.value = null
      }

      message.success('已移除选中元素')
    }
  }

  /**
   * 清除所有选中的元素
   */
  const clearSelectedElements = () => {
    // 通知iframe清除所有选中状态
    selectedElements.forEach((element) => {
      sendMessageToIframe('element-deselected', {
        xpath: element.xpath,
        elementId: element.id,
      })
    })

    selectedElements.length = 0
    activeElement.value = null
    hoveredElement.value = null
  }

  /**
   * 进入编辑模式
   */
  const enterEditMode = () => {
    isEditMode.value = true
    sendMessageToIframe('enable-edit-mode')
    message.info('已进入可视化编辑模式')
  }

  /**
   * 退出编辑模式
   */
  const exitEditMode = () => {
    isEditMode.value = false
    sendMessageToIframe('disable-edit-mode')
    clearSelectedElements()
    message.info('已退出可视化编辑模式')
  }

  /**
   * 切换编辑模式
   */
  const toggleEditMode = () => {
    if (isEditMode.value) {
      exitEditMode()
    } else {
      enterEditMode()
    }
  }

  /**
   * 设置iframe引用
   * @param iframe iframe元素
   */
  const setIframeRef = (iframe: HTMLIFrameElement | null) => {
    iframeRef.value = iframe
  }

  /**
   * 生成包含选中元素信息的提示词
   * @param originalMessage 原始消息
   * @returns 包含元素信息的完整提示词
   */
  const generatePromptWithElements = (originalMessage: string): string => {
    if (selectedElements.length === 0) {
      return originalMessage
    }

    const elementsInfo = selectedElements
      .map((element) => {
        return `元素信息:
- 标签: ${element.tagName}
- 类名: ${element.className || '无'}
- 文本内容: ${element.textContent || '无'}
- CSS选择器: ${element.selector}
- 属性: ${
          Object.entries(element.attributes)
            .map(([key, value]) => `${key}="${value}"`)
            .join(', ') || '无'
        }`
      })
      .join('\n\n')

    return `${originalMessage}

请基于以下选中的页面元素进行修改:

${elementsInfo}

请针对这些选中的元素进行相应的修改或优化。`
  }

  /**
   * 获取选中元素的摘要信息
   * @returns 摘要信息数组
   */
  const getSelectedElementsSummary = () => {
    return selectedElements.map((element) => ({
      id: element.id,
      display: `${element.tagName}${element.className ? '.' + element.className : ''}`,
      text: element.textContent
        ? ` - "${element.textContent.substring(0, 30)}${element.textContent.length > 30 ? '...' : ''}"`
        : '',
    }))
  }

  // 组件挂载时添加消息监听器
  onMounted(() => {
    window.addEventListener('message', handleIframeMessage)
  })

  // 组件卸载时移除监听器和清理状态
  onUnmounted(() => {
    window.removeEventListener('message', handleIframeMessage)
    clearSelectedElements()
  })

  return {
    // 状态
    isEditMode,
    selectedElements,
    hoveredElement,
    activeElement,

    // 方法
    toggleEditMode,
    enterEditMode,
    exitEditMode,
    removeSelectedElement,
    clearSelectedElements,
    setIframeRef,
    generatePromptWithElements,
    getSelectedElementsSummary,
    sendMessageToIframe,
  }
}

/// <reference types="vite/client" />
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  // 使用宽松的组件类型声明以通过类型检查
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

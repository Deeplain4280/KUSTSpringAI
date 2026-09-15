/// <reference types="vite/client" />

// .vue SFC 兜底声明：避免个别 SFC 未被 vue-tsc 解析时，import 处报 TS7016
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const component: DefineComponent<Record<string, never>, Record<string, never>, any>
  export default component
}

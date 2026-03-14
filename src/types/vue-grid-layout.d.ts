declare module 'vue-grid-layout' {
  import type { DefineComponent } from 'vue'

  export const GridLayout: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>
  export const GridItem: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>
}

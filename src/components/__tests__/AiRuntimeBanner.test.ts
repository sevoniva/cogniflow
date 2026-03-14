import ElementPlus from 'element-plus'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AiRuntimeBanner from '@/components/AiRuntimeBanner.vue'

describe('AiRuntimeBanner', () => {
  it('renders semantic fallback status', () => {
    const wrapper = mount(AiRuntimeBanner, {
      props: {
        status: {
          mode: 'semantic',
          enabled: false,
          runtimeEnabled: false,
          reason: '未启用外部大模型，系统当前使用业务语义引擎',
          defaultProvider: 'kimi',
          providerName: 'Kimi',
          model: null
        }
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    expect(wrapper.text()).toContain('业务语义引擎模式')
    expect(wrapper.text()).toContain('Kimi')
    expect(wrapper.text()).toContain('Semantic BI')
  })

  it('renders runtime enabled status', () => {
    const wrapper = mount(AiRuntimeBanner, {
      props: {
        status: {
          mode: 'llm',
          enabled: true,
          runtimeEnabled: true,
          reason: '外部大模型已就绪',
          defaultProvider: 'kimi',
          providerName: 'Kimi',
          model: 'moonshot-v1-32k'
        }
      },
      global: {
        plugins: [ElementPlus]
      }
    })

    expect(wrapper.text()).toContain('外部模型已启用')
    expect(wrapper.text()).toContain('moonshot-v1-32k')
    expect(wrapper.text()).toContain('Ready')
  })
})

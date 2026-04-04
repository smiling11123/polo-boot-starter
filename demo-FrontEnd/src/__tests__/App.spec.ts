import { describe, expect, it } from 'vitest'

import { mount } from '@vue/test-utils'
import App from '../App.vue'

describe('App', () => {
  it('renders router view shell', () => {
    const wrapper = mount(App, {
      global: {
        stubs: {
          RouterView: {
            template: '<div>upload-workbench</div>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('upload-workbench')
  })
})

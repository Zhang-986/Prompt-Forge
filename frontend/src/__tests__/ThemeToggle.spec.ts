import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ThemeToggle from '../components/ThemeToggle.vue'

describe('ThemeToggle.vue', () => {
    beforeEach(() => {
        // 清理 localStorage
        localStorage.clear()
        // 重置 DOM 主题属性
        document.documentElement.removeAttribute('data-theme')
    })

    it('renders correctly with default theme', () => {
        const wrapper = mount(ThemeToggle)
        expect(wrapper.find('.theme-toggle').exists()).toBe(true)
        expect(wrapper.find('.theme-icon').exists()).toBe(true)
        expect(wrapper.find('.theme-label').exists()).toBe(true)
    })

    it('displays system theme label by default', () => {
        const wrapper = mount(ThemeToggle)
        expect(wrapper.find('.theme-label').text()).toBe('跟随系统')
    })

    it('cycles through themes on click', async () => {
        const wrapper = mount(ThemeToggle)

        // Initial: system
        expect(wrapper.find('.theme-label').text()).toBe('跟随系统')

        // Click 1: light
        await wrapper.find('.theme-toggle').trigger('click')
        await nextTick()
        expect(wrapper.find('.theme-label').text()).toBe('浅色')

        // Click 2: dark
        await wrapper.find('.theme-toggle').trigger('click')
        await nextTick()
        expect(wrapper.find('.theme-label').text()).toBe('深色')

        // Click 3: back to system
        await wrapper.find('.theme-toggle').trigger('click')
        await nextTick()
        expect(wrapper.find('.theme-label').text()).toBe('跟随系统')
    })

    it('persists theme to localStorage', async () => {
        const wrapper = mount(ThemeToggle)

        await wrapper.find('.theme-toggle').trigger('click')

        expect(localStorage.getItem('theme')).toBe('light')
    })

    it('restores theme from localStorage', () => {
        localStorage.setItem('theme', 'dark')

        const wrapper = mount(ThemeToggle)

        expect(wrapper.find('.theme-label').text()).toBe('深色')
    })

    it('applies theme to document element', async () => {
        const wrapper = mount(ThemeToggle)

        // Click to set light theme
        await wrapper.find('.theme-toggle').trigger('click')
        await nextTick()

        expect(document.documentElement.getAttribute('data-theme')).toBe('light')
    })

    it('shows correct icon for each theme', async () => {
        const wrapper = mount(ThemeToggle)

        // System theme shows 💻
        expect(wrapper.find('.theme-icon').text()).toBe('💻')

        // Light theme shows ☀️
        await wrapper.find('.theme-toggle').trigger('click')
        await nextTick()
        expect(wrapper.find('.theme-icon').text()).toBe('☀️')

        // Dark theme shows 🌙
        await wrapper.find('.theme-toggle').trigger('click')
        await nextTick()
        expect(wrapper.find('.theme-icon').text()).toBe('🌙')
    })
})

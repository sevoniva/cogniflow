import { describe, it, expect } from 'vitest'
import { ChatBarChart, ChatLineChart, ChatPieChart } from '@/components/Chart'

describe('Chart Components', () => {
  describe('ChatBarChart', () => {
    it('should be defined', () => {
      expect(ChatBarChart).toBeDefined()
    })
  })

  describe('ChatLineChart', () => {
    it('should be defined', () => {
      expect(ChatLineChart).toBeDefined()
    })
  })

  describe('ChatPieChart', () => {
    it('should be defined', () => {
      expect(ChatPieChart).toBeDefined()
    })
  })
})

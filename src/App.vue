<template>
  <router-view />
</template>

<script setup lang="ts">
</script>

<style>
@import './styles/variables.css';

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: var(--cb-font-family);
  background: var(--cb-bg-hero);
  background-attachment: fixed;
  color: var(--cb-text-primary);
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 通用页面容器 */
.cb-page {
  min-height: 100vh;
  background: transparent;
}

.cb-page-container {
  max-width: var(--cb-user-max-width);
  margin: 0 auto;
  padding: var(--cb-space-xl) var(--cb-space-md);
}

.cb-page-container--wide {
  max-width: var(--cb-user-wide-max-width);
}

/* 通用内容区 */
.cb-content {
  display: flex;
  flex-direction: column;
  gap: var(--cb-space-lg);
}

/* 工具栏 */
.cb-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--cb-space-md);
  flex-wrap: wrap;
}

/* 操作按钮组 */
.cb-actions {
  display: flex;
  justify-content: center;
  gap: var(--cb-space-md);
  margin-top: var(--cb-space-xl);
  flex-wrap: wrap;
}

/* 筛选栏 */
.cb-filter-bar {
  display: flex;
  gap: var(--cb-space-md);
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: var(--cb-space-md);
}

/* 响应式 */
@media (max-width: 640px) {
  .cb-page-container {
    padding: var(--cb-space-lg) var(--cb-space-md);
  }
  
  .cb-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .cb-toolbar .el-input,
  .cb-toolbar .el-select {
    width: 100% !important;
  }
}

/* Element Plus 全局样式微调 */

/* 按钮统一 */
.el-button {
  font-weight: var(--cb-font-weight-medium);
  border-radius: 12px;
}

.el-button--primary {
  --el-button-bg-color: var(--cb-primary);
  --el-button-border-color: var(--cb-primary);
  --el-button-hover-bg-color: var(--cb-primary-dark);
  --el-button-hover-border-color: var(--cb-primary-dark);
  --el-button-active-bg-color: var(--cb-primary-dark);
}

/* 表格 */
.el-table {
  --el-table-border-color: var(--cb-border-lighter);
}

.el-table th {
  background: linear-gradient(to bottom, #fafbfc, #f5f7fa) !important;
  font-weight: var(--cb-font-weight-bold);
  color: var(--cb-text-primary);
}

/* 输入框 */
.el-input__wrapper {
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(47, 107, 255, 0.06);
}

.el-input__wrapper.is-focus {
  box-shadow: var(--cb-shadow-focus);
}

/* 对话框 */
.el-dialog {
  border-radius: var(--cb-radius-xl);
  overflow: hidden;
  box-shadow: var(--cb-shadow-lg);
}

.el-dialog__header {
  background: linear-gradient(to bottom, #fafbfc, #f5f7fa);
  border-bottom: 1px solid var(--cb-border-lighter);
  margin: 0;
  padding: var(--cb-space-md) var(--cb-space-lg);
}

.el-dialog__title {
  font-weight: var(--cb-font-weight-bold);
  color: var(--cb-text-primary);
}

.el-dialog__body {
  padding: var(--cb-space-lg);
}

.el-dialog__footer {
  border-top: 1px solid var(--cb-border-lighter);
  padding: var(--cb-space-md) var(--cb-space-lg);
}

/* 空状态 */
.el-empty {
  padding: var(--cb-space-xxl) 0;
}

.el-empty__description {
  color: var(--cb-text-secondary);
  margin-top: var(--cb-space-md);
}

/* 标签 */
.el-tag {
  font-weight: var(--cb-font-weight-medium);
}

#app {
  min-height: 100vh;
}
</style>

<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <p class="copyright">
        <a
          href="https://github.com/zw-zhong-bot/zw-ai-code-mother.git"
          target="_blank"
          rel="noopener noreferrer"
          class="author-link"
        >
          应用生成平台
        </a>
      </p>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
// 无需额外的响应式数据
</script>

<style scoped>
.footer {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  text-align: center;
  padding: 20px;
  margin-top: 40px;
  border-top: 1px solid rgba(102, 126, 234, 0.1);
}

.copyright {
  margin: 0;
  color: #666;
  font-size: 14px;
}

@media (max-width: 768px) {
  .footer {
    padding: 16px 12px;
    margin-top: 24px;
    /* 适配全面屏底部手势条 */
    padding-bottom: calc(16px + var(--zw-safe-bottom));
  }

  .copyright {
    font-size: 13px;
  }
}
</style>

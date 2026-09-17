# ============================================================
# zw-ai-code-mother 后端运行时镜像
#
# 设计前提：**jar 在本地编译好**，这里只做运行时打包，不编译任何代码。
# 因此镜像里没有 Maven、没有 JDK，只有 JRE + AI 生成功能所需的运行时依赖。
#
# 构建上下文 = 服务器上的 /opt/zw-ai-code-mother/backend
#   ├── Dockerfile
#   ├── docker-compose.yml
#   ├── backend.env        （服务器侧创建，chmod 600，不随发布传输）
#   └── app.jar            （每次发版由本地 release.sh 覆盖上传）
#
# 层级顺序是刻意安排的：所有安装层在前、COPY app.jar 在最后。
# 这样每次发版只有最后一层变动，前面全部命中缓存，构建只需几秒，
# 也避免在 2GB 内存的服务器上反复跑 apt。
# ============================================================

FROM eclipse-temurin:21-jre-jammy

ARG INSTALL_CHROMIUM=false

# ---- 依赖层 1：Node 20 + cnpm ----
# AI 生成的 Vue 项目要执行 cnpm install / cnpm run build（VueProjectBuilder 里命令写死为 cnpm）。
# Ubuntu jammy 自带的 nodejs 是 12.x，跑不了 Vite 7，必须换成 Node 20。
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates gnupg \
 && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
 && apt-get install -y --no-install-recommends nodejs \
 && npm config set registry https://registry.npmmirror.com \
 && npm install -g cnpm --registry=https://registry.npmmirror.com \
 && apt-get purge -y gnupg \
 && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/* \
 && node -v && cnpm -v

# ---- 依赖层 2：中文字体 ----
# 部署截图里的中文不能渲染成方块。
RUN apt-get update \
 && apt-get install -y --no-install-recommends fonts-noto-cjk \
 && rm -rf /var/lib/apt/lists/*

# ---- 依赖层 3：可选 Chromium ----
# WebScreenshotUtils 的 Chrome 初始化在静态块里，但该类是懒加载的 —— 只有首次部署截图
# （或凌晨 2 点的清理任务）才触发，不影响应用启动。
# 服务器只有 2GB 内存，默认不装，服务照常跑，只是部署后不会自动生成封面（手动上传仍可用）。
# 需要时：docker compose build --build-arg INSTALL_CHROMIUM=true
RUN if [ "$INSTALL_CHROMIUM" = "true" ]; then \
      curl -fsSL -o /tmp/chrome.deb \
        https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
      && apt-get update \
      && apt-get install -y --no-install-recommends /tmp/chrome.deb \
      && rm -f /tmp/chrome.deb \
      && rm -rf /var/lib/apt/lists/* ; \
    else \
      echo "[skip] 未安装 Chromium，部署截图功能不可用" ; \
    fi

WORKDIR /app

# ---- 目录层（必须在 COPY jar 之前，否则每次发版都会失效）----
# 代码里所有路径都基于 System.getProperty("user.dir")，即这里的 WORKDIR。
# 这些目录在容器启动前会被 volume 挂载覆盖，但必须先存在。
RUN mkdir -p /app/tmp/code_output \
             /app/tmp/code_deploy \
             /app/temp/screenshot \
             /app/logs \
             /app/src/main/resources/static/fengmian \
             /app/src/main/resources/static/ping

# ---- 应用层：每次发版唯一变动的层 ----
COPY app.jar app.jar

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8

EXPOSE 8123

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=60", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.io.tmpdir=/app/tmp", \
  "-Dfile.encoding=UTF-8", \
  "-Duser.timezone=Asia/Shanghai", \
  "-jar", "app.jar"]

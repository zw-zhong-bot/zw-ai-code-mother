#!/usr/bin/env bash
# ============================================================
# 一键发版（在开发机执行）
#
#   ./deploy/scripts/release.sh                    # 构建 + 上传 + 部署
#   ./deploy/scripts/release.sh --skip-build       # 复用最近一次构建产物
#   ./deploy/scripts/release.sh --version <ver>    # 指定已有产物
#
# 流程：
#   本地编译 jar 与 dist  →  scp 上传  →  服务器重建镜像层（秒级，命中缓存）→ 切前端软链接 → 健康检查
#
# 可覆盖的变量（也可写进 deploy/release.env，该文件已被 gitignore）：
#   SERVER_HOST  默认 203.195.211.168
#   SERVER_USER  默认 root
#   SSH_KEY      默认 W:/ssh/zw_ai_code.pem
#   ROOT         默认 /opt/zw-ai-code-mother
# ============================================================
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
DEPLOY_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
REPO_DIR=$(cd "$DEPLOY_DIR/.." && pwd)

# shellcheck disable=SC1091
[[ -f "$DEPLOY_DIR/release.env" ]] && . "$DEPLOY_DIR/release.env"

SERVER_HOST=${SERVER_HOST:-203.195.211.168}
SERVER_USER=${SERVER_USER:-root}
SSH_KEY=${SSH_KEY:-W:/ssh/zw_ai_code.pem}
ROOT=${ROOT:-/opt/zw-ai-code-mother}

SKIP_BUILD=false
VERSION_ARG=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build) SKIP_BUILD=true; shift ;;
    --version) VERSION_ARG="${2:-}"; shift 2 ;;
    *) echo "未知参数: $1" >&2; exit 1 ;;
  esac
done

head1() { echo; echo "=== $* ==="; }
info()  { echo "  $*"; }
die()   { echo "[fail] $*" >&2; exit 1; }

SSH_OPTS=(-i "$SSH_KEY" -o BatchMode=yes -o StrictHostKeyChecking=accept-new
          -o ConnectTimeout=20 -o ServerAliveInterval=15)

# ------------------------------------------------------------
head1 "0/5 预检"

[[ -f "$SSH_KEY" ]] || die "找不到私钥：$SSH_KEY"

# 直接测真实连接，而不是先探测私钥可读性。
# 原因：ssh-keygen -y 在嵌套 shell / 脚本环境下行为不稳定（本机实测返回 255 且无任何输出），
# 用它做前置判定会产生误报、把正常的发布流程挡住。ssh 自身报错更准。
info "连接服务器 …"
SSH_PROBE=$(ssh "${SSH_OPTS[@]}" "$SERVER_USER@$SERVER_HOST" 'echo __PROBE_OK__' 2>&1) || true

if ! grep -q '__PROBE_OK__' <<<"$SSH_PROBE"; then
  if grep -qiE 'UNPROTECTED PRIVATE KEY|are too open|bad permissions' <<<"$SSH_PROBE"; then
    cat >&2 <<EOF

[fail] 私钥权限过宽，Windows OpenSSH 拒绝使用它。

在 PowerShell 或 CMD 里执行下面两条收紧权限（仅授权当前用户），然后重跑：

  icacls "$(cygpath -w "$SSH_KEY" 2>/dev/null || printf '%s' "$SSH_KEY")" /inheritance:r
  icacls "$(cygpath -w "$SSH_KEY" 2>/dev/null || printf '%s' "$SSH_KEY")" /grant:r "%USERNAME%:R"

EOF
  fi
  echo "[fail] 无法连接 $SERVER_USER@$SERVER_HOST" >&2
  printf '%s\n' "$SSH_PROBE" | sed 's/^/       /' >&2
  exit 1
fi
info "服务器连通: $SERVER_USER@$SERVER_HOST"

# 首次发版要求服务器上已有 backend.env
if ! ssh "${SSH_OPTS[@]}" "$SERVER_USER@$SERVER_HOST" "test -f $ROOT/backend/backend.env" 2>/dev/null; then
  cat >&2 <<EOF
[fail] 服务器上还没有 $ROOT/backend/backend.env

请先执行一次环境初始化：
  ssh -i "$SSH_KEY" $SERVER_USER@$SERVER_HOST
  mkdir -p $ROOT/backend
  cp ...（首次可用本仓库 deploy/backend.env.example 作模板）
  vi $ROOT/backend/backend.env && chmod 600 $ROOT/backend/backend.env
EOF
  exit 1
fi
info "服务器 backend.env 已存在"

# ------------------------------------------------------------
head1 "1/5 构建"

if $SKIP_BUILD; then
  if [[ -n "$VERSION_ARG" ]]; then
    # 指定版本 + 跳过构建：直接取该版本目录（用于发布自己手动构建的产物）
    BUILD_DIR="$DEPLOY_DIR/build/$VERSION_ARG"
    [[ -d "$BUILD_DIR" ]] || die "找不到指定版本的构建产物：$BUILD_DIR"
  else
    BUILD_DIR=$(ls -1dt "$DEPLOY_DIR"/build/*/ 2>/dev/null | head -1 || true)
    [[ -n "$BUILD_DIR" ]] || die "deploy/build/ 下没有构建产物，请先执行 ./deploy/scripts/build-local.sh"
    BUILD_DIR="${BUILD_DIR%/}"
  fi
  VERSION=$(basename "$BUILD_DIR")
  info "跳过构建，复用: $VERSION"
elif [[ -n "$VERSION_ARG" ]]; then
  # 用 export 而不是 `VERSION=x cmd` 前缀赋值：某些受限 shell（如被包装过的 CI/沙箱环境）
  # 不会传递前缀赋值，export 则一定生效。
  export VERSION="$VERSION_ARG"
  "$SCRIPT_DIR/build-local.sh"
  BUILD_DIR="$DEPLOY_DIR/build/$VERSION_ARG"
  VERSION="$VERSION_ARG"
else
  "$SCRIPT_DIR/build-local.sh"
  BUILD_DIR=$(ls -1dt "$DEPLOY_DIR"/build/*/ | head -1)
  BUILD_DIR="${BUILD_DIR%/}"
  VERSION=$(basename "$BUILD_DIR")
fi

[[ -f "$BUILD_DIR/app.jar" ]]     || die "缺少 $BUILD_DIR/app.jar"
[[ -f "$BUILD_DIR/dist.tar.gz" ]] || die "缺少 $BUILD_DIR/dist.tar.gz"
info "版本: $VERSION"

# ------------------------------------------------------------
head1 "2/5 上传后端"

# 同时同步 Dockerfile / compose / 回滚脚本，保证服务器上的定义与仓库一致
ssh "${SSH_OPTS[@]}" "$SERVER_USER@$SERVER_HOST" "mkdir -p $ROOT/backend $ROOT/scripts"
scp -q "${SSH_OPTS[@]}" "$DEPLOY_DIR/Dockerfile" "$DEPLOY_DIR/docker-compose.yml" \
    "$SERVER_USER@$SERVER_HOST:$ROOT/backend/" || die "上传 Dockerfile/compose 失败"
scp -q "${SSH_OPTS[@]}" "$SCRIPT_DIR/rollback.sh" \
    "$SERVER_USER@$SERVER_HOST:$ROOT/scripts/" || die "上传 rollback.sh 失败"
ssh "${SSH_OPTS[@]}" "$SERVER_USER@$SERVER_HOST" "chmod +x $ROOT/scripts/rollback.sh"
info "Dockerfile / docker-compose.yml / rollback.sh 已同步"

scp -q "${SSH_OPTS[@]}" "$BUILD_DIR/app.jar" \
    "$SERVER_USER@$SERVER_HOST:$ROOT/backend/app.jar" || die "上传 app.jar 失败"
info "app.jar 已上传 ($(du -h "$BUILD_DIR/app.jar" | cut -f1))"

# ------------------------------------------------------------
head1 "3/5 上传前端"

REMOTE_TAR="/tmp/zw-dist-$VERSION.tar.gz"
scp -q "${SSH_OPTS[@]}" "$BUILD_DIR/dist.tar.gz" \
    "$SERVER_USER@$SERVER_HOST:$REMOTE_TAR" || die "上传前端产物失败"
info "dist 已上传 ($(du -h "$BUILD_DIR/dist.tar.gz" | cut -f1))"

# ------------------------------------------------------------
head1 "4/5 服务器部署"

ssh "${SSH_OPTS[@]}" "$SERVER_USER@$SERVER_HOST" \
    "VERSION='$VERSION' ROOT='$ROOT' REMOTE_TAR='$REMOTE_TAR' bash -s" <<'REMOTE'
set -euo pipefail
echo "  [1] 重建后端镜像 tag=$VERSION（仅最后一层变动，应很快）"
cd "$ROOT/backend"
export TAG="$VERSION"
docker compose build
docker compose up -d

echo "  [2] 等待健康检查（最多 150s）"
HEALTHY=false
for i in $(seq 1 50); do
  if curl -fsS --max-time 3 http://127.0.0.1:8123/api/health/ >/dev/null 2>&1; then
    HEALTHY=true; echo "      健康检查通过（约 $((i*3))s）"; break
  fi
  sleep 3
done

if ! $HEALTHY; then
  echo "  [fail] 后端健康检查超时，最近日志："
  docker compose logs --tail 60 backend
  echo "  [rollback] 回滚到上一个可用镜像"
  PREV=$(cat "$ROOT/backend/.last-good-tag" 2>/dev/null || echo "")
  if [ -n "$PREV" ]; then export TAG="$PREV"; docker compose up -d; echo "      已回滚到 $PREV"; fi
  exit 1
fi
echo "$VERSION" > "$ROOT/backend/.last-good-tag"

echo "  [3] 发布前端 $VERSION"
REL="$ROOT/web/releases/$VERSION"
mkdir -p "$REL"
tar xzf "$REMOTE_TAR" -C "$REL"
rm -f "$REMOTE_TAR"
[ -f "$REL/index.html" ] || { echo "  [fail] 前端产物里没有 index.html"; exit 1; }
ln -sfn "$REL" "$ROOT/web/current.tmp"
mv -Tf "$ROOT/web/current.tmp" "$ROOT/web/current"
echo "      current -> $(readlink -f "$ROOT/web/current")"

echo "  [4] 清理旧版本"
ls -1dt "$ROOT/web/releases"/* 2>/dev/null | tail -n +6 | xargs -r rm -rf
docker image prune -f --filter "until=168h" >/dev/null 2>&1 || true

echo "  [5] 当前状态"
echo "      后端 tag : $(cat "$ROOT/backend/.last-good-tag")"
echo "      容器     : $(docker inspect -f '{{.State.Status}} ({{.State.Health.Status}})' zw-backend 2>/dev/null)"
REMOTE

# ------------------------------------------------------------
head1 "5/5 完成"
echo
info "版本      : $VERSION"
info "访问地址  : http://$SERVER_HOST/"
info "接口自检  : curl http://$SERVER_HOST/api/health/"
info "查看版本  : ssh -i \"$SSH_KEY\" $SERVER_USER@$SERVER_HOST '$ROOT/scripts/rollback.sh list'"
info "回滚      : ssh -i \"$SSH_KEY\" $SERVER_USER@$SERVER_HOST '$ROOT/scripts/rollback.sh frontend|backend'"

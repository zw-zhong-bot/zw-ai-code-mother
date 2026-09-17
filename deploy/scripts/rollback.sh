#!/usr/bin/env bash
# ============================================================
# 回滚脚本（在服务器上执行）
#
# 由 release.sh 自动上传到 /opt/zw-ai-code-mother/scripts/
# 本地执行方式：
#   ssh -i <key> root@<host> '/opt/zw-ai-code-mother/scripts/rollback.sh list'
#   ssh -i <key> root@<host> '/opt/zw-ai-code-mother/scripts/rollback.sh frontend'
#   ssh -i <key> root@<host> '/opt/zw-ai-code-mother/scripts/rollback.sh backend'
# ============================================================
set -euo pipefail

ROOT=${ROOT:-/opt/zw-ai-code-mother}
WEB_ROOT="$ROOT/web"
IMAGE=${IMAGE:-zw-ai-code-mother-backend}

switch_frontend() {
  local target="$1"
  ln -sfn "$target" "$WEB_ROOT/current.tmp"
  mv -Tf "$WEB_ROOT/current.tmp" "$WEB_ROOT/current"
}

case "${1:-}" in
  frontend)
    PREV=$(ls -1dt "$WEB_ROOT/releases"/* 2>/dev/null | sed -n 2p)
    [[ -n "$PREV" ]] || { echo "[fail] 没有更早的前端版本可回滚"; exit 1; }
    switch_frontend "$PREV"
    echo "[ok] 前端已回滚到 $(basename "$PREV")（毫秒级，无需重载 nginx）"
    ;;

  backend)
    TAG=$(cat "$ROOT/backend/.last-good-tag" 2>/dev/null || true)
    [[ -n "$TAG" ]] || { echo "[fail] 找不到 $ROOT/backend/.last-good-tag"; exit 1; }
    cd "$ROOT/backend"
    export TAG
    docker compose up -d
    echo "[ok] 后端已回滚到镜像 tag: $TAG"
    ;;

  list)
    echo "=== 前端可用版本（新 → 旧）==="
    ls -1dt "$WEB_ROOT/releases"/* 2>/dev/null | head -10 || echo "  (无)"
    echo "  当前指向: $(readlink -f "$WEB_ROOT/current" 2>/dev/null || echo '未设置')"
    echo
    echo "=== 后端可用镜像 ==="
    docker images "$IMAGE" --format '  {{.Tag}}  {{.CreatedSince}}  {{.Size}}' 2>/dev/null | head -10
    echo "  上次成功: $(cat "$ROOT/backend/.last-good-tag" 2>/dev/null || echo '无记录')"
    echo
    echo "=== 容器状态 ==="
    docker inspect -f '  zw-backend: {{.State.Status}} (health: {{.State.Health.Status}})' zw-backend 2>/dev/null \
      || echo "  zw-backend 未运行"
    ;;

  *)
    echo "用法: $0 {frontend|backend|list}"
    exit 1
    ;;
esac

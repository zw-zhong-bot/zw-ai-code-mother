#!/usr/bin/env bash
# ============================================================
# 服务器首次环境初始化（在服务器上执行，只碰环境，不动项目代码）
#
# 由本地执行：
#   scp -i <key> deploy/scripts/server-bootstrap.sh root@<host>:/root/
#   ssh -i <key> root@<host> \
#     'MYSQL_ROOT_PASSWORD=xx APP_DB_PASSWORD=yy ADMIN_PASSWORD=zz bash /root/server-bootstrap.sh'
#
# 可选参数：
#   --check-only        只体检，不做任何修改
#   --sql <path>        建表脚本路径；仅在三张表不存在时需要
#
# 幂等：可重复执行。
#
# ⚠️ 本机 MySQL 为 26.7.0，已移除 MD5() / SHA1() 等函数，
#    因此管理员密码哈希在 shell 侧用 md5sum 计算后再写入，
#    不能用 MD5(CONCAT('zwpass','...')) 那种写法（会报 ERROR 1305）。
# ============================================================
set -euo pipefail

ROOT=${ROOT:-/opt/zw-ai-code-mother}
DB=zw_ai_code_mother
MC=${MYSQL_CONTAINER:-}
APP_DB_USER=${APP_DB_USER:-zw_app}
ADMIN_ACCOUNT=${ADMIN_ACCOUNT:-admin}
CODE_DEPLOY_HOST=${CODE_DEPLOY_HOST:-}

CHECK_ONLY=false
SQL_FILE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --check-only) CHECK_ONLY=true; shift ;;
    --sql) SQL_FILE="${2:-}"; shift 2 ;;
    *) echo "未知参数: $1" >&2; exit 1 ;;
  esac
done

R=$'\033[31m'; G=$'\033[32m'; Y=$'\033[33m'; D=$'\033[2m'; N=$'\033[0m'
ok()   { echo "${G}[ok]${N}   $*"; }
warn() { echo "${Y}[warn]${N} $*"; }
fail() { echo "${R}[fail]${N} $*"; }
info() { echo "${D}       $*${N}"; }
step() { echo; echo "=== $* ==="; }
die()  { fail "$*"; exit 1; }

# ------------------------------------------------------------
step "1/6 体检"

. /etc/os-release 2>/dev/null && info "系统: ${PRETTY_NAME:-unknown}"
info "CPU : $(nproc) 核"
awk '/MemTotal/{printf "       内存: %.0f MB\n",$2/1024}' /proc/meminfo
df -h "$(dirname "$ROOT")" 2>/dev/null | awk 'NR==2{print "       磁盘: 可用 "$4}' || true

command -v docker >/dev/null 2>&1 || die "未安装 docker"
docker info >/dev/null 2>&1 || die "docker 守护进程未运行"
ok "docker: $(docker --version)"

# MySQL 容器探测
if [[ -z "$MC" ]]; then
  MC=$(docker ps --filter "publish=3306" --format '{{.Names}}' 2>/dev/null | head -1)
fi
if [[ -z "$MC" ]]; then
  MC=$(docker ps --format '{{.Names}}\t{{.Image}}' | grep -i mysql | cut -f1 | head -1)
fi
[[ -n "$MC" ]] || { docker ps --format '  {{.Names}} {{.Image}}'; die "未探测到 MySQL 容器，请用 MYSQL_CONTAINER=xxx 指定"; }
ok "MySQL 容器: $MC（$(docker inspect -f '{{.Config.Image}}' "$MC")）"

if redis-cli -h 127.0.0.1 -p 6379 ping 2>/dev/null | grep -q PONG; then
  ok "Redis: PONG"
else
  warn "Redis 未响应 127.0.0.1:6379（后端 session 与对话记忆依赖它）"
fi

if ss -lntp 2>/dev/null | grep -q ':8123'; then
  warn "端口 8123 已被占用（可能有旧容器在跑）"
else
  ok "端口 8123 空闲"
fi

$CHECK_ONLY && { echo; ok "体检完成（--check-only，未做任何修改）"; exit 0; }

# ------------------------------------------------------------
step "2/6 参数校验"

for pair in "MYSQL_ROOT_PASSWORD:MySQL root 密码" \
            "APP_DB_PASSWORD:应用账号 ${APP_DB_USER} 的密码" \
            "ADMIN_PASSWORD:管理员 ${ADMIN_ACCOUNT} 的登录密码"; do
  var=${pair%%:*}; desc=${pair#*:}
  [[ -n "${!var:-}" ]] || die "$var 未设置（$desc）"
done
ok "参数齐备"

ROOT_PW="$MYSQL_ROOT_PASSWORD"

# 统一封装：SQL 一律走 stdin，避免多行 -e 被拆词；
# 不需要 stdin 的调用必须显式 < /dev/null，否则 docker exec -i 会吞掉脚本后续内容。
sq()  { docker exec -e MYSQL_PWD="$ROOT_PW" "$MC" mysql -uroot --default-character-set=utf8mb4 -N "$@" < /dev/null; }
sqh() { docker exec -e MYSQL_PWD="$ROOT_PW" "$MC" mysql -uroot --default-character-set=utf8mb4 "$@" < /dev/null; }
sqt() { docker exec -i -e MYSQL_PWD="$ROOT_PW" "$MC" mysql -uroot --default-character-set=utf8mb4 "$@"; }

sq -e "SELECT 1;" >/dev/null 2>&1 || die "root 连接 MySQL 失败，请检查 MYSQL_ROOT_PASSWORD 与容器名"
ok "MySQL root 连接正常（版本 $(sq -e 'SELECT VERSION();' 2>/dev/null)）"

# ------------------------------------------------------------
step "3/6 目录树"

for d in "$ROOT/backend" "$ROOT/scripts" \
         "$ROOT/data/code_output" "$ROOT/data/apps" \
         "$ROOT/data/temp/screenshot" "$ROOT/data/fengmian" "$ROOT/data/ping" \
         "$ROOT/web/releases" "$ROOT/logs"; do
  mkdir -p "$d"
done
ok "已就绪：$ROOT/{backend,scripts,data/*,web/releases,logs}"

# ------------------------------------------------------------
step "4/6 数据库"

SQL_EXISTS=$(sq -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$DB' AND TABLE_NAME IN ('user','app','chat_history');" 2>/dev/null || echo 0)

if [[ "$SQL_EXISTS" == "3" ]]; then
  ok "库 $DB 的三张表已存在，跳过建表"
else
  [[ -n "$SQL_FILE" && -f "$SQL_FILE" ]] || die "库 $DB 的表不完整（找到 $SQL_EXISTS/3），请用 --sql <建表脚本路径> 指定 sql.sql"
  info "导入 $SQL_FILE …"
  docker exec -i -e MYSQL_PWD="$ROOT_PW" "$MC" \
    mysql -uroot --default-character-set=utf8mb4 < "$SQL_FILE"
  ok "建表完成"
fi

# userRole 枚举修正：sql.sql 里是 enum('vip')，代码注册时写入 'user'，严格模式会报 1265
sq -e "ALTER TABLE $DB.user MODIFY COLUMN userRole ENUM('user','admin','vip') NULL DEFAULT NULL COMMENT '用户角色：user/admin/vip';"
ok "userRole = $(sq -e "SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$DB' AND TABLE_NAME='user' AND COLUMN_NAME='userRole';")"

# 应用账号
sqt <<SQL
CREATE USER IF NOT EXISTS '${APP_DB_USER}'@'127.0.0.1' IDENTIFIED BY '${APP_DB_PASSWORD}';
ALTER USER '${APP_DB_USER}'@'127.0.0.1' IDENTIFIED BY '${APP_DB_PASSWORD}';
GRANT ALL PRIVILEGES ON ${DB}.* TO '${APP_DB_USER}'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
ok "应用账号 ${APP_DB_USER}@127.0.0.1（插件 $(sq -e "SELECT plugin FROM mysql.user WHERE user='${APP_DB_USER}';")）"

# 管理员：哈希在 shell 侧算（MySQL 26.7.0 无 MD5()）
ADMIN_HASH=$(printf '%s' "zwpass${ADMIN_PASSWORD}" | md5sum | cut -d' ' -f1)
sqt "$DB" <<SQL
INSERT INTO user (userAccount, userPassword, userName, userRole, userProfile)
VALUES ('${ADMIN_ACCOUNT}', '${ADMIN_HASH}', '管理员', 'admin', '云端部署管理员')
ON DUPLICATE KEY UPDATE
  userPassword = '${ADMIN_HASH}',
  userRole     = 'admin';
SQL
GOT=$(sq "$DB" -e "SELECT userPassword FROM user WHERE userAccount='${ADMIN_ACCOUNT}';" | tr -d '\r\n ')
[[ "$GOT" == "$ADMIN_HASH" ]] && ok "管理员 ${ADMIN_ACCOUNT} 已就绪（哈希校验一致）" || die "管理员哈希写入不一致"

# ------------------------------------------------------------
step "5/6 生成 backend.env"

ENV_FILE="$ROOT/backend/backend.env"
if [[ -f "$ENV_FILE" ]]; then
  ok "已存在，不覆盖：$ENV_FILE"
  info "如需更新数据库密码请手动编辑"
else
  [[ -n "$CODE_DEPLOY_HOST" ]] || CODE_DEPLOY_HOST="http://$(curl -fsS --max-time 5 ifconfig.me 2>/dev/null || echo '<SERVER_IP>')/apps"
  cat > "$ENV_FILE" <<EOF
# 由 server-bootstrap.sh 生成于 $(date '+%Y-%m-%d %H:%M:%S')
# 敏感值请自行补齐；本文件权限已设为 600
SPRING_PROFILES_ACTIVE=prod

MYSQL_USER=${APP_DB_USER}
MYSQL_PASSWORD=${APP_DB_PASSWORD}
REDIS_PASSWORD=

# ↓↓↓ 以下三项必须替换为真实值，否则 AI 生成 / 截图上传功能不可用 ↓↓↓
DEEPSEEK_API_KEY=CHANGE_ME
COS_HOST=https://CHANGE_ME.cos.ap-guangzhou.myqcloud.com
COS_SECRET_ID=CHANGE_ME
COS_SECRET_KEY=CHANGE_ME
COS_BUCKET=CHANGE_ME

# AI 生成应用的对外访问前缀
CODE_DEPLOY_HOST=${CODE_DEPLOY_HOST}
EOF
  chmod 600 "$ENV_FILE"
  ok "已生成 $ENV_FILE（权限 600）"
  warn "还需手动补齐 DEEPSEEK_API_KEY 与 COS 四项"
fi

# ------------------------------------------------------------
step "6/6 验证"

echo "--- 三张表 ---"
sqt "$DB" -t -e "SHOW TABLES;" 2>/dev/null

echo "--- 管理员 ---"
sqt "$DB" -t -e "SELECT id, userAccount, userName, userRole FROM user;" 2>/dev/null

echo "--- 用应用账号独立连接 ---"
docker exec -e MYSQL_PWD="$APP_DB_PASSWORD" "$MC" mysql -h127.0.0.1 -u"$APP_DB_USER" -t "$DB" \
  -e "SELECT COUNT(*) AS user_rows FROM user;" < /dev/null 2>/dev/null && ok "应用账号可用" || die "应用账号连接失败"

echo
ok "环境初始化完成"
echo
info "下一步（在开发机执行）："
info "  1) 补齐 $ENV_FILE 里的 DEEPSEEK_API_KEY 与 COS 四项"
info "  2) ./deploy/scripts/release.sh"

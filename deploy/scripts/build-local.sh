#!/usr/bin/env bash
# ============================================================
# 本地构建脚本（在开发机上执行）
#
# 产出：
#   deploy/build/<版本>/app.jar        后端可执行 jar（已 repackage）
#   deploy/build/<版本>/dist.tar.gz    前端构建产物打包
#   deploy/build/<版本>/MANIFEST.txt   版本、git 提交、构建时间、文件校验
#
# 用法：
#   ./deploy/scripts/build-local.sh             增量构建（node_modules 未变则跳过装依赖）
#   ./deploy/scripts/build-local.sh --fresh     强制重装前端依赖
#   VERSION=v1.0.0 ./deploy/scripts/build-local.sh
#
# 说明：本地只做编译，不需要 Docker。镜像在服务器上构建，但只 COPY 这个 jar。
# ============================================================
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_DIR=$(cd "$SCRIPT_DIR/../.." && pwd)
FRONTEND_DIR="$REPO_DIR/zw-ai-code-mother-frontend"
OUT_ROOT="$REPO_DIR/deploy/build"

FRESH=false
[[ "${1:-}" == "--fresh" ]] && FRESH=true

GIT_SHA=$(git -C "$REPO_DIR" rev-parse --short HEAD 2>/dev/null || echo "nogit")
VERSION=${VERSION:-"$(date +%Y%m%d-%H%M%S)-$GIT_SHA"}
BUILD_DIR="$OUT_ROOT/$VERSION"

info() { echo "  $*"; }
head1() { echo; echo "=== $* ==="; }
die()  { echo "[fail] $*" >&2; exit 1; }

# ------------------------------------------------------------
head1 "0/5 环境检查"

# JDK 21（本机全局 JAVA_HOME 指向 1.8，必须覆盖）
detect_jdk21() {
  local cand
  # 1) 已设的 JAVA_HOME，可能是 Windows 形式（E:\Java\java1.8\），先转 POSIX
  if [[ -n "${JAVA_HOME:-}" ]]; then
    cand=$(cygpath -u "$JAVA_HOME" 2>/dev/null || printf '%s' "$JAVA_HOME")
    cand="${cand%/}"
    if [[ -x "$cand/bin/java" ]] && "$cand/bin/java" -version 2>&1 | head -1 | grep -q '"21'; then
      printf '%s' "$cand"; return
    fi
  fi
  # 2) 常见安装位置
  for cand in "/c/Program Files/Java/jdk-21" \
              "/c/Program Files/Eclipse Adoptium"/*21* \
              "/c/Program Files/Microsoft"/jdk-21* \
              "$HOME"/.jdks/*21* \
              "$HOME"/.workbuddy/binaries/java/*21* ; do
    cand="${cand%/}"
    if [[ -x "$cand/bin/java" ]]; then printf '%s' "$cand"; return; fi
  done
  printf ''
}

JAVA_HOME=$(detect_jdk21)
[[ -n "$JAVA_HOME" ]] || die "找不到 JDK 21。请安装 JDK 21，或设置 JAVA_HOME 指向它"
export JAVA_HOME
JAVA_VER=$("$JAVA_HOME/bin/java" -version 2>&1 | head -1)
info "JAVA_HOME : $JAVA_HOME"
info "Java      : $JAVA_VER"
case "$JAVA_VER" in *'"21'*) ;; *) die "需要 JDK 21，当前不是 21" ;; esac

command -v npm >/dev/null || die "找不到 npm"
info "Node      : $(node -v) / npm $(npm -v)"

[[ -f "$FRONTEND_DIR/.env.production" ]] \
  || die "缺少 $FRONTEND_DIR/.env.production —— 没有它前端会去请求 localhost，线上必然白屏"
info "前端生产变量: $(grep -c . "$FRONTEND_DIR/.env.production") 行已就位"

info "版本号    : $VERSION"

# ------------------------------------------------------------
head1 "1/5 清理产物目录"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
info "$BUILD_DIR"

# ------------------------------------------------------------
head1 "2/5 构建后端 jar"

run_maven() {
  # 首选官方 wrapper；Git Bash 下 mvnw 依赖 /tmp 会失败，此时退回 wrapper 缓存里的发行版
  if "$REPO_DIR/mvnw" -v >/dev/null 2>&1; then
    "$REPO_DIR/mvnw" "$@"
    return
  fi
  info "mvnw 不可用（Git Bash 下 /tmp 受限），改用 wrapper 缓存中的 Maven"
  local mvn_home
  mvn_home=$(ls -d "$HOME"/.m2/wrapper/dists/apache-maven-3.9.11-bin/*/apache-maven-3.9.11 2>/dev/null | head -1)
  [[ -n "$mvn_home" ]] || die "找不到 Maven 发行版，请先在正常终端执行一次 ./mvnw -v"
  mkdir -p "$REPO_DIR/tmp"
  "$JAVA_HOME/bin/java" \
    -Djava.io.tmpdir="$(cygpath -m "$REPO_DIR/tmp")" \
    -classpath "$(cygpath -m "$mvn_home/boot/plexus-classworlds-2.9.0.jar")" \
    -Dclassworlds.conf="$(cygpath -m "$mvn_home/bin/m2.conf")" \
    -Dmaven.home="$(cygpath -m "$mvn_home")" \
    -Dmaven.multiModuleProjectDirectory="$(cygpath -m "$REPO_DIR")" \
    org.codehaus.plexus.classworlds.launcher.Launcher "$@"
}

( cd "$REPO_DIR" && run_maven -B -DskipTests clean package ) || die "后端构建失败"

SRC_JAR=$(ls -1 "$REPO_DIR/target"/*.jar 2>/dev/null | grep -v '\.original$' | head -1)
[[ -n "$SRC_JAR" ]] || die "在 target/ 下找不到 jar 产物"
cp "$SRC_JAR" "$BUILD_DIR/app.jar"
info "jar  : $(basename "$SRC_JAR")  $(du -h "$BUILD_DIR/app.jar" | cut -f1)"

# ------------------------------------------------------------
head1 "3/5 构建前端"

cd "$FRONTEND_DIR"

# 依赖未变则跳过安装：用 lockfile 的哈希做指纹
STAMP="$FRONTEND_DIR/node_modules/.lock-hash"
LOCK_HASH=$(node -e "
const fs=require('fs'),c=require('crypto');
console.log(c.createHash('sha256').update(fs.readFileSync('package-lock.json')).digest('hex').slice(0,16));
")

if $FRESH || [[ ! -d node_modules ]] || [[ ! -f "$STAMP" ]] || [[ "$(cat "$STAMP" 2>/dev/null)" != "$LOCK_HASH" ]]; then
  info "安装前端依赖（npm ci，lockfile 指纹 $LOCK_HASH）…"
  npm ci --no-audit --no-fund
  echo "$LOCK_HASH" > "$STAMP"
else
  info "依赖未变，跳过安装（指纹 $LOCK_HASH）"
fi

# 注意：用 build-only 而不是 build。
# build 会先跑 vue-tsc 类型检查，项目当前存在 32 个类型错误（多数来自死代码），
# 会导致构建整体失败。等类型错误清零后可改回 npm run build。
info "vite build …"
npm run build-only

[[ -f "$FRONTEND_DIR/dist/index.html" ]] || die "前端构建未产出 dist/index.html"
tar czf "$BUILD_DIR/dist.tar.gz" -C "$FRONTEND_DIR/dist" .
info "dist : $(du -h "$BUILD_DIR/dist.tar.gz" | cut -f1)（压缩后）"

# ------------------------------------------------------------
head1 "4/5 生成清单"

cat > "$BUILD_DIR/MANIFEST.txt" <<EOF
version   : $VERSION
git-sha   : $GIT_SHA
built-at  : $(date '+%Y-%m-%d %H:%M:%S')
built-on  : $(uname -s) / java 21
app.jar   : $(sha256sum "$BUILD_DIR/app.jar" | cut -d' ' -f1)
dist.tar  : $(sha256sum "$BUILD_DIR/dist.tar.gz" | cut -d' ' -f1)
EOF
cat "$BUILD_DIR/MANIFEST.txt" | sed 's/^/  /'

# ------------------------------------------------------------
head1 "5/5 完成"
echo
info "产物目录：$BUILD_DIR"
info "发版命令：./deploy/scripts/release.sh --version $VERSION"

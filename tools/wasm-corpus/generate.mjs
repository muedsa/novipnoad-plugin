// 用 V8 生成 i32 语料：每个测试点一个导出函数，期望值由 V8 实际执行得到。
// 生成物供 Kotlin 侧 WasmI32CorpusTest 做差分校验（我们的解释器 vs 浏览器引擎）。
//
// 用法：node tools/wasm-corpus/generate.mjs
// 输出：app/src/test/resources/wasm/i32-corpus.wasm
//       app/src/test/resources/wasm/i32-corpus.tsv   (name \t args \t expected)
import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const I32 = 0x7f;
const OUT_DIR = join(dirname(fileURLToPath(import.meta.url)), '../../app/src/test/resources/wasm');

// ---------- 二进制编码 ----------
const lebU = (value) => {
  const out = [];
  let v = value >>> 0;
  do {
    let b = v & 0x7f;
    v >>>= 7;
    if (v) b |= 0x80;
    out.push(b);
  } while (v);
  return out;
};

const lebS = (value) => {
  const out = [];
  let v = value | 0;
  for (;;) {
    const b = v & 0x7f;
    v >>= 7;
    if ((v === 0 && !(b & 0x40)) || (v === -1 && (b & 0x40))) {
      out.push(b);
      return out;
    }
    out.push(b | 0x80);
  }
};

const vecBytes = (bytes) => [...lebU(bytes.length), ...bytes];
const str = (s) => vecBytes([...s].map((c) => c.charCodeAt(0)));
const section = (id, payload) => [id, ...lebU(payload.length), ...payload];

// ---------- 模块构造 ----------
// funcs: [{ name, params: [valtype], results: [valtype], locals: [[count, valtype]], body: [byte] }]
function buildModule(funcs, dataSegments = []) {
  const typeKeys = new Map();
  const types = [];
  const typeIndexOf = (f) => {
    const key = `${f.params.join(',')}|${f.results.join(',')}`;
    if (!typeKeys.has(key)) {
      typeKeys.set(key, types.length);
      types.push([0x60, ...vecBytes(f.params), ...vecBytes(f.results)]);
    }
    return typeKeys.get(key);
  };

  const typeIdx = funcs.map(typeIndexOf);
  const bodies = funcs.map((f) => {
    const groups = f.locals ?? [];
    const content = [
      ...lebU(groups.length),
      ...groups.flatMap(([count, type]) => [...lebU(count), type]),
      ...f.body,
      0x0b,
    ];
    return [...lebU(content.length), ...content];
  });

  const exportEntries = funcs.flatMap((f, i) => [...str(f.name), 0x00, ...lebU(i)]);
  const dataEntries = dataSegments.flatMap(({ offset, bytes }) => [
    0x00, 0x41, ...lebS(offset), 0x0b, ...vecBytes(bytes),
  ]);

  const parts = [
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
    section(1, [...lebU(types.length), ...types.flat()]),
    section(3, vecBytes(typeIdx.flatMap((t) => lebU(t)))),
    section(5, [0x01, 0x00, 0x01]),                 // 1 个 memory，min = 1 页
    section(7, [...lebU(funcs.length), ...exportEntries]),
    section(10, [...lebU(bodies.length), ...bodies.flat()]),
  ];
  if (dataEntries.length) parts.push(section(11, [...lebU(dataSegments.length), ...dataEntries]));
  return new Uint8Array(parts.flat());
}

// ---------- 用例定义 ----------
const BIN_OPS = {
  add: 0x6a, sub: 0x6b, mul: 0x6c,
  div_s: 0x6d, div_u: 0x6e, rem_s: 0x6f, rem_u: 0x70,
  and: 0x71, or: 0x72, xor: 0x73,
  shl: 0x74, shr_s: 0x75, shr_u: 0x76, rotl: 0x77, rotr: 0x78,
};
const CMP_OPS = {
  eq: 0x46, ne: 0x47, lt_s: 0x48, lt_u: 0x49, gt_s: 0x4a,
  gt_u: 0x4b, le_s: 0x4c, le_u: 0x4d, ge_s: 0x4e, ge_u: 0x4f,
};
const UNARY_OPS = {
  eqz: 0x45, clz: 0x67, ctz: 0x68, popcnt: 0x69,
  extend8_s: 0xc0, extend16_s: 0xc1,
};

const funcs = [
  // 固定放在下标 0：供 ctl_call_add1 用 call 0 调用
  { name: '_add1', params: [I32], results: [I32], body: [0x20, 0x00, 0x41, 0x01, 0x6a] },
];
const cases = [];

const binaryFunc = (name, op) => {
  funcs.push({ name, params: [I32, I32], results: [I32], body: [0x20, 0x00, 0x20, 0x01, op] });
};
const unaryFunc = (name, op) => {
  funcs.push({ name, params: [I32], results: [I32], body: [0x20, 0x00, op] });
};

for (const [name, op] of Object.entries(BIN_OPS)) binaryFunc(`bin_${name}`, op);
for (const [name, op] of Object.entries(CMP_OPS)) binaryFunc(`cmp_${name}`, op);
for (const [name, op] of Object.entries(UNARY_OPS)) unaryFunc(`un_${name}`, op);

// 二元用例的参数对：覆盖 0/1/-1/边界/无符号边界/移位量/循环位移量
const PAIRS = [
  [0, 0], [1, 1], [1, 0], [0, 1], [2, 3], [7, 2], [-7, 2], [7, -2], [-7, -2],
  [-1, 1], [1, -1], [-1, -1], [-1, 0], [0, -1],
  [0x7fffffff, 1], [1, 0x7fffffff], [-0x80000000, 1], [1, -0x80000000],
  [-0x80000000, -1], [-1, -0x80000000],
  [0x12345678, 4], [0x12345678, 36], [0x12345678, -4],
  [0x80000001, 1], [-1, 31], [-1, 32], [-1, 33], [1, -33],
  [100, 7], [-100, 7], [100, -7],
  [0x0f0f0f0f, 0x00ff00ff], [0x55555555, 0xaaaaaaaa],
];

// 一元用例：覆盖符号位、符号扩展边界、clz/ctz/popcnt 的极端值
const VALUES = [
  0, 1, -1, 2, -2, 7, -7, 31, 32, 33,
  0x7fffffff, -0x80000000, 0x80000001, 0x12345678,
  0x0000007f, 0x00000080, 0x00007fff, 0x00008000, 0x0000ffff, 0x00ff00ff,
];

for (const name of Object.keys(BIN_OPS)) {
  for (const [a, b] of PAIRS) cases.push({ name: `bin_${name}`, args: [a, b] });
}
for (const name of Object.keys(CMP_OPS)) {
  for (const [a, b] of PAIRS) cases.push({ name: `cmp_${name}`, args: [a, b] });
}
for (const name of Object.keys(UNARY_OPS)) {
  for (const a of VALUES) cases.push({ name: `un_${name}`, args: [a] });
}

// ---------- 控制流 / 栈 / 内存用例 ----------
const ctl = (name, params, results, locals, body, argSets) => {
  funcs.push({ name, params, results, locals, body });
  for (const args of argSets) cases.push({ name, args });
};

// block 带结果，br 携带栈值跳出
ctl('ctl_block_br_value', [I32], [I32], [], [
  0x02, 0x7f, 0x20, 0x00, 0x0c, 0x00, 0x41, 0x07, 0x0b,
], [[0], [1], [-1], [0x7fffffff]]);

// br 穿透一层 block（深度 1），带值
ctl('ctl_nested_br', [I32], [I32], [], [
  0x02, 0x7f, 0x02, 0x40, 0x20, 0x00, 0x0c, 0x01, 0x0b, 0x41, 0x2a, 0x0b,
], [[0], [5], [-5]]);

// br_if 携带栈值；条件为 0 时落到 drop + 常量
ctl('ctl_br_if_value', [I32, I32], [I32], [], [
  0x02, 0x7f, 0x20, 0x00, 0x20, 0x01, 0x0d, 0x00, 0x1a, 0x41, 0x63, 0x0b,
], [[5, 1], [5, 0], [5, -1], [-3, 2], [-3, 0]]);

// if / else 带结果
ctl('ctl_if_result', [I32, I32], [I32], [], [
  0x20, 0x00, 0x04, 0x7f, 0x20, 0x01, 0x05, 0x41, ...lebS(-1), 0x0b,
], [[1, 42], [0, 42], [-1, 42], [2, -7]]);

// if 无 else（条件为假时直接跳过）
ctl('ctl_if_no_else', [I32], [I32], [], [
  0x20, 0x00, 0x04, 0x40, 0x01, 0x0b, 0x41, 0x05,
], [[0], [1], [-1]]);

// if 分支内部 br 到外层 block（带值），覆盖 else 与 br 的交错
ctl('ctl_if_br_outer', [I32, I32], [I32], [], [
  0x02, 0x7f,
  0x20, 0x00, 0x04, 0x7f, 0x20, 0x01, 0x0c, 0x01, 0x05, 0x41, ...lebS(-1), 0x0b,
  0x0b,
], [[1, 42], [0, 42], [-1, 42], [7, -9]]);

// br 到函数体标签（深度 2），等价于提前 return
ctl('ctl_br_function', [I32], [I32], [], [
  0x02, 0x40, 0x02, 0x40, 0x20, 0x00, 0x0c, 0x02, 0x0b, 0x0b, 0x41, 0x07,
], [[0], [5], [-5]]);

// loop + br_if 跳出 + br 回跳：从 a 累加到 1
ctl('ctl_loop_sum', [I32], [I32], [[1, I32]], [
  0x02, 0x40,
  0x03, 0x40,
  0x20, 0x00, 0x41, 0x00, 0x4c, 0x0d, 0x01,
  0x20, 0x01, 0x20, 0x00, 0x6a, 0x21, 0x01,
  0x20, 0x00, 0x41, 0x01, 0x6b, 0x21, 0x00,
  0x0c, 0x00,
  0x0b, 0x0b,
  0x20, 0x01,
], [[0], [1], [5], [100], [-3]]);

// select：cond 非 0 取第一个
ctl('ctl_select', [I32, I32, I32], [I32], [], [
  0x20, 0x00, 0x20, 0x01, 0x20, 0x02, 0x1b,
], [[1, 2, 1], [1, 2, 0], [1, 2, -1], [-1, -2, 3]]);

// local.tee：写回局部变量同时保留栈值
ctl('ctl_tee', [I32], [I32], [[1, I32]], [
  0x41, 0x0a, 0x21, 0x01, 0x20, 0x00, 0x22, 0x01, 0x20, 0x01, 0x6a,
], [[0], [5], [-3]]);

ctl('ctl_drop', [I32], [I32], [], [0x20, 0x00, 0x41, 0x05, 0x1a], [[0], [9]]);
ctl('ctl_return', [I32], [I32], [], [0x20, 0x00, 0x0f, 0x41, 0x07], [[0], [5]]);

// 本地函数调用（下标 0 是 _add1）
ctl('ctl_call_add1', [I32], [I32], [], [0x20, 0x00, 0x10, 0x00],
  [[0], [1], [-1], [0x7fffffff]]);

// 内存：data 段写入 0..7，load8_u 读取（含 offset 立即数）
ctl('mem_load8', [I32], [I32], [], [0x20, 0x00, 0x2d, 0x00, 0x00],
  [[0], [1], [2], [3], [4], [100], [65536], [-1]]);
ctl('mem_load8_offset', [I32], [I32], [], [0x20, 0x00, 0x2d, 0x00, 0x01],
  [[0], [1], [2], [65535]]);
ctl('mem_store8_readback', [I32, I32], [I32], [], [
  0x20, 0x00, 0x20, 0x01, 0x3a, 0x00, 0x00,
  0x20, 0x00, 0x2d, 0x00, 0x00,
], [[256, 0xff], [257, 0x100], [258, -1], [65535, 7], [65536, 1], [-1, 1]]);

// trap：unreachable
ctl('trap_unreachable', [], [I32], [], [0x00], [[]]);

// void 函数
ctl('void_noop', [I32], [], [], [], [[1], [0]]);

// ---------- 生成 + 用 V8 求期望值 ----------
const wasm = buildModule(funcs, [{ offset: 0, bytes: [0x00, 0x7f, 0x80, 0xff, 0x11, 0x22, 0x33, 0x44] }]);
const instance = new WebAssembly.Instance(new WebAssembly.Module(wasm));

const lines = ['# 由 tools/wasm-corpus/generate.mjs 生成，期望值来自 V8；请勿手改', '# name\targs\texpected'];
const mismatches = [];
let trapped = 0;
for (const { name, args } of cases) {
  const fn = instance.exports[name];
  if (typeof fn !== 'function') throw new Error(`缺少导出: ${name}`);
  let expected;
  try {
    const value = fn(...args);
    expected = value === undefined ? 'void' : String(value | 0);
  } catch (e) {
    if (!(e instanceof WebAssembly.RuntimeError)) throw e;
    expected = 'trap';
    trapped++;
  }
  if (expected === 'trap') mismatches.push(`${name}(${args}) trap`);
  // 参数按 i32 语义写出（|0），否则 0x80000001 之类的字面量会超出 Kotlin Int
  const argsAsI32 = args.map((v) => v | 0);
  lines.push(`${name}\t${argsAsI32.join(',')}\t${expected}`);
}

mkdirSync(OUT_DIR, { recursive: true });
writeFileSync(join(OUT_DIR, 'i32-corpus.wasm'), wasm);
writeFileSync(join(OUT_DIR, 'i32-corpus.tsv'), lines.join('\n') + '\n', 'utf8');

console.log(`module: ${wasm.length} bytes, functions: ${funcs.length}`);
console.log(`cases: ${cases.length} (trap: ${trapped})`);
console.log(`trap 用例: ${mismatches.length}`);

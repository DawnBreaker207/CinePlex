const fs = require("fs");
const path = require("path");

const vi = JSON.parse(fs.readFileSync("public/i18n/vi.json", "utf8"));
const en = JSON.parse(fs.readFileSync("public/i18n/en.json", "utf8"));
const viKeys = Object.keys(vi).filter((k) => !k.includes("--- FILE"));
const enKeys = Object.keys(en).filter((k) => !k.includes("--- FILE"));

const glob = (dir, exts) => {
  let out = [];
  for (const f of fs.readdirSync(dir)) {
    const p = path.join(dir, f);
    const s = fs.statSync(p);
    if (s.isDirectory()) out = out.concat(glob(p, exts));
    else if (exts.some((e) => p.endsWith(e))) out.push(p);
  }
  return out;
};

const files = glob("src", [".html", ".ts"]);
const patterns = [
  /\{\{\s*['"]([^'"]+)['"]\s*\|\s*translate/g,
  /marker\(['"]([^'"]+)['"]\)/g,
  /translate\.instant\(['"]([^'"]+)['"]\)/g,
  /\[[a-zA-Z]+\]="['"]([^'"]+)['"]\s*\|\s*translate/g,
  /['"]([^'"]+)['"]\s*\|\s*translate/g,
];
const used = new Set();
files.forEach((f) => {
  const c = fs.readFileSync(f, "utf8");
  patterns.forEach((p) => {
    let m;
    while ((m = p.exec(c)) !== null) {
      const k = m[1].trim();
      if (k && !k.includes("{{")) used.add(k);
    }
  });
});

const stale = viKeys.filter((k) => !used.has(k));
const missingVi = [...used].filter((k) => !viKeys.includes(k));
const missingEn = viKeys.filter((k) => !enKeys.includes(k));

let errors = 0;
if (missingVi.length) {
  errors += missingVi.length;
  console.log("MISSING in vi.json (used in code but absent):");
  missingVi.forEach((k) => console.log("  -", JSON.stringify(k)));
}
if (missingEn.length) {
  errors += missingEn.length;
  console.log("MISSING in en.json (in vi.json but absent):");
  missingEn.forEach((k) => console.log("  -", JSON.stringify(k)));
}
if (stale.length) {
  console.log("STALE keys (in vi.json, not used in code):", stale.length);
  stale.forEach((k) => console.log("  -", JSON.stringify(k)));
}
console.log(`\nvi: ${viKeys.length} keys | en: ${enKeys.length} keys | used: ${used.size}`);
if (errors) {
  console.log(`\nFAIL: ${errors} missing key(s)`);
  process.exit(1);
}
console.log("OK: all used keys present in vi.json + en.json");

const fs = require("fs");
const path = require("path");
const glob = require("glob");

// Cấu hình đường dẫn
const INPUT_PATH = "./src/**/*.{html,ts}";
const OUTPUT_FILES = ["./public/i18n/vi.json"];

const patterns = [
  /{{[\s]*['"]([^'"]+)['"][\s]*\|[\s]*translate/g,
  /marker\(['"]([^'"]+)['"]\)/g,
  /translate\.instant\(['"]([^'"]+)['"]\)/g,
  /\[nzErrorTip\]="['"]([^'"]+)['"]\s*\|\s*translate/g,
  /\[placeholder\]="['"]([^'"]+)['"]\s*\|\s*translate/g,
];

let extractedData = {};
let allKeys = new Set();

const files = glob.sync(INPUT_PATH);
console.log(`--- Đang quét ${files.length} files ---`);

files.forEach((file) => {
  const content = fs.readFileSync(file, "utf8");
  const relativePath = path.relative(path.join(process.cwd(), "src/app"), file);
  const sectionLabel = `--- FILE: ${relativePath.replace(/\\/g, "/")} ---`;

  patterns.forEach((pattern) => {
    let match;
    while ((match = pattern.exec(content)) !== null) {
      const key = match[1].trim();
      // Loại bỏ các Key rác hoặc key chứa biến động {{ }}
      if (key && !key.includes("{{") && !allKeys.has(key)) {
        if (!extractedData[sectionLabel])
          extractedData[sectionLabel] = new Set();
        extractedData[sectionLabel].add(key);
        allKeys.add(key);
      }
    }
  });
});

OUTPUT_FILES.forEach((outputPath) => {
  const dir = path.dirname(outputPath);
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });

  let existingData = {};
  if (fs.existsSync(outputPath)) {
    try {
      existingData = JSON.parse(fs.readFileSync(outputPath, "utf8"));
    } catch (e) {
      existingData = {};
    }
  }

  const finalJson = {};
  const sortedSections = Object.keys(extractedData).sort();

  sortedSections.forEach((section) => {
    // 1. Tạo nhãn phân cách
    finalJson[section] = "#";

    const keys = Array.from(extractedData[section]).sort();
    keys.forEach((key) => {
      // Giữ lại bản dịch cũ nếu có
      finalJson[key] = existingData[key] || key;
    });
  });

  // 2. Chuyển Object thành chuỗi JSON
  let jsonString = JSON.stringify(finalJson, null, 2);

  // 3. THỦ THUẬT: Dùng Regex để chèn dòng trống trước mỗi nhãn "--- FILE:"
  // Tìm cụm '  "--- FILE:' và thay bằng '\n  "--- FILE:'
  const spacedJson = jsonString.replace(/  "--- FILE:/g, '\n  "--- FILE:');

  fs.writeFileSync(outputPath, spacedJson, "utf8");
  console.log(`✔️ Đã cập nhật xong (có dòng trống): ${outputPath}`);
});

console.log("--- Hoàn tất! ---");

import * as XLSX from "xlsx";

export function exportSheet(filename, sheetName, headers, rows) {
  const safeName = (sheetName || "Sheet1").slice(0, 31);
  const ws = XLSX.utils.aoa_to_sheet([headers, ...rows]);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, safeName);
  const base = filename.endsWith(".xlsx") ? filename : `${filename}.xlsx`;
  XLSX.writeFile(wb, base);
}

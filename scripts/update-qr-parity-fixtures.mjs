import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const root = fileURLToPath(new URL("../", import.meta.url));

const api = await import(
  pathToFileURL(
    join(
      root,
      "qrcode-web/build/packages/js/styled-qr-kmp-qrcode-web.mjs",
    ),
  ).href
);

const bytes = (count) =>
  Array.from(
    { length: count },
    (_, index) => (index * 73 + 19) & 255,
  );

const cases = [
  { id: "empty-text", text: "", bytes: [], version: 1, level: "L" },
  { id: "empty-bytes", text: null, bytes: [], version: 1, level: "H" },
  { id: "ascii-text", text: "x", bytes: [], version: 1, level: "H" },
  {
    id: "unicode-text",
    text: "Привет, QR 👋",
    bytes: [],
    version: 4,
    level: "Q",
  },
  {
    id: "unsigned-bytes",
    text: null,
    bytes: [0, 1, 127, 128, 254, 255],
    version: 1,
    level: "H",
  },
  {
    id: "level-l",
    text: null,
    bytes: bytes(70),
    version: 4,
    level: "L",
    expectedLevel: "L",
  },
  {
    id: "upgraded-to-m",
    text: null,
    bytes: bytes(60),
    version: 4,
    level: "L",
    expectedLevel: "M",
  },
  {
    id: "level-q",
    text: null,
    bytes: bytes(40),
    version: 4,
    level: "Q",
    expectedLevel: "Q",
  },
  {
    id: "version-information",
    text: null,
    bytes: bytes(48),
    version: 7,
    level: "H",
  },
];

function rows(size, read) {
  return Array.from({ length: size }, (_, row) =>
    Array.from({ length: size }, (_, column) =>
      read(row, column) ? "1" : "0",
    ).join(""),
  );
}

const snapshots = cases.map((entry) => {
  const options = new api.StyledQrEncodingOptions();
  options.errorCorrection = entry.level;
  options.minVersion = entry.version;
  options.maxVersion = entry.version;

  const sourceBytes =
    entry.text === null
      ? entry.bytes
      : Array.from(new TextEncoder().encode(entry.text));

  const matrix =
    entry.text === null
      ? api.encodeStyledQrBytes(sourceBytes, options)
      : api.encodeStyledQrText(entry.text, options);

  const decoded = api.decodeStyledQrMatrix(matrix.copyModules());
  const actualBytes = decoded.copyBytes();

  if (
    matrix.version !== entry.version ||
    decoded.version !== matrix.version ||
    decoded.errorCorrection !== matrix.errorCorrection ||
    (entry.expectedLevel !== undefined &&
      matrix.errorCorrection !== entry.expectedLevel) ||
    decoded.correctedErrorCount !== 0 ||
    actualBytes.length !== sourceBytes.length ||
    !actualBytes.every((value, index) => value === sourceBytes[index]) ||
    (decoded.utf8TextOrNull() ?? null) !== entry.text
  ) {
    throw new Error("Cannot freeze invalid fixture: " + entry.id);
  }

  console.log(
    entry.id +
      ": version=" +
      matrix.version +
      ", requested=" +
      entry.level +
      ", actual=" +
      matrix.errorCorrection,
  );

  return {
    id: entry.id,
    text: entry.text,
    bytes: sourceBytes,
    requestedErrorCorrection: entry.level,
    minVersion: entry.version,
    maxVersion: entry.version,
    version: matrix.version,
    errorCorrection: matrix.errorCorrection,
    size: matrix.size,
    mask: decoded.mask,
    eciAssignment: decoded.eciAssignment ?? null,
    utf8Text: decoded.utf8TextOrNull() ?? null,
    moduleRows: rows(matrix.size, (r, c) => matrix.get(r, c)),
    functionRows: rows(
      matrix.size,
      (r, c) => matrix.isFunctionModule(r, c),
    ),
    locatorRows: rows(
      matrix.size,
      (r, c) => matrix.isLocatorModule(r, c),
    ),
  };
});

function kt(value) {
  if (value === null) return "null";

  if (typeof value === "string") {
    return JSON.stringify(value).replaceAll("$", "\\$");
  }

  return String(value);
}

function ktRows(values) {
  return (
    "arrayOf(\n" +
    values
      .map((value) => "            " + kt(value) + ",")
      .join("\n") +
    "\n        )"
  );
}

const fields = [
  "id",
  "text",
  "requestedErrorCorrection",
  "minVersion",
  "maxVersion",
  "version",
  "errorCorrection",
  "size",
  "mask",
  "eciAssignment",
  "utf8Text",
];

const kotlin = [
  "// Frozen contract snapshots. Regenerate only as an explicit reviewed change.",
  "package io.github.eugeneponomarev.styledqr.fixtures",
  "",
  "internal data class QrParitySnapshot(",
  "    val id: String,",
  "    val text: String?,",
  "    val bytes: IntArray,",
  "    val requestedErrorCorrection: String,",
  "    val minVersion: Int,",
  "    val maxVersion: Int,",
  "    val version: Int,",
  "    val errorCorrection: String,",
  "    val size: Int,",
  "    val mask: Int,",
  "    val eciAssignment: Int?,",
  "    val utf8Text: String?,",
  "    val moduleRows: Array<String>,",
  "    val functionRows: Array<String>,",
  "    val locatorRows: Array<String>,",
  ")",
  "",
  "internal object QrParitySnapshots {",
  "    val all: List<QrParitySnapshot> = listOf(",
  ...snapshots.flatMap((entry) => [
    "        QrParitySnapshot(",
    ...fields.map(
      (field) =>
        "            " + field + " = " + kt(entry[field]) + ",",
    ),
    "            bytes = intArrayOf(" + entry.bytes.join(", ") + "),",
    "            moduleRows = " + ktRows(entry.moduleRows) + ",",
    "            functionRows = " + ktRows(entry.functionRows) + ",",
    "            locatorRows = " + ktRows(entry.locatorRows) + ",",
    "        ),",
  ]),
  "    )",
  "}",
  "",
].join("\n");

const fixtureRoot = join(root, "tests/fixtures");
const kotlinDirectory = join(
  fixtureRoot,
  "kotlin/io/github/eugeneponomarev/styledqr/fixtures",
);

mkdirSync(kotlinDirectory, { recursive: true });

writeFileSync(
  join(fixtureRoot, "qr-parity.json"),
  JSON.stringify(snapshots, null, 2) + "\n",
);

writeFileSync(
  join(kotlinDirectory, "QrParitySnapshots.kt"),
  kotlin,
);

console.log("Frozen " + snapshots.length + " QR parity fixtures.");
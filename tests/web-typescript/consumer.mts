import qrParityFixtures from "./qr-parity.json" with { type: "json" };
import {
  StyledQrOptions,
  StyledQrLogoOptions,
  StyledQrGradientOptions,
  StyledQrExportOptions,
  StyledQrEncodingOptions,
  StyledQrScanabilityOptions,
  analyzeStyledQrScanability,
  encodeStyledQrText,
  encodeStyledQrBytes,
  decodeStyledQrMatrix,
  generateStyledQrSvg,
  generateStyledQrSvgWithOptions,
  generateThemedQrSvg,
  getStyledQrThemeIds,
  renderStyledQrSvg,
  renderStyledQrSvgForExport,
  renderStyledQrPng,
} from "styled-qr-kmp-web";

import type {
  StyledQrMatrix,
  StyledQrDecodedPayload,
  StyledQrSvgResult,
  StyledQrPngResult,
  StyledQrWebError,
} from "styled-qr-kmp-web";

function check(
  condition: unknown,
  message: string,
): asserts condition {
  if (!condition) throw new Error(message);
}

function required<T>(
  value: T | null | undefined,
  message: string,
): T {
  if (value === null || value === undefined) {
    throw new Error(message);
  }

  return value;
}

function sameBytes(
  actual: readonly number[],
  expected: readonly number[],
): void {
  check(
    actual.length === expected.length &&
      actual.every((value, index) => value === expected[index]),
    "Decoded bytes differ",
  );
}

function expectThrow(action: () => unknown): void {
  let thrown = false;

  try {
    action();
  } catch {
    thrown = true;
  }

  check(thrown, "Expected an exception");
}

function svgOf(result: StyledQrSvgResult): string {
  check(
    result.isSuccess,
    result.error?.message ?? "Expected successful SVG generation",
  );
  check(!result.isEmpty, "Successful SVG cannot be empty");

  return required(result.svg, "SVG is missing");
}

function expectError(
  result: StyledQrSvgResult,
  expectedCode: string,
): void {
  check(!result.isSuccess, "Error result cannot be successful");
  check(!result.isEmpty, "Error result cannot be empty");

  const error: StyledQrWebError = required(
    result.error,
    "Error details are missing",
  );

  check(error.code === expectedCode, "Unexpected error code: " + error.code);
  check(error.message.trim().length > 0, "Error message is empty");
}

// Text, matrix and public metadata.
const encoding = new StyledQrEncodingOptions();
encoding.errorCorrection = "M";
encoding.minVersion = 4;
encoding.maxVersion = 4;

const text = "Привет, QR 👋";
const matrix: StyledQrMatrix = encodeStyledQrText(text, encoding);

check(matrix.version === 4, "Unexpected QR version");
check(matrix.size === 33, "Unexpected matrix size");
check(matrix.isFunctionModule(0, 0), "Finder must be a function module");
check(matrix.isLocatorModule(0, 0), "Finder must be a locator module");
check(matrix.isFunctionModule(6, 8), "Timing module must be structural");
check(!matrix.isLocatorModule(6, 8), "Timing module must not be a locator");

const modules: boolean[][] = matrix.copyModules();

check(Array.isArray(modules), "Matrix must be a JavaScript array");
check(
  modules.every(
    (row) =>
      Array.isArray(row) &&
      row.length === matrix.size &&
      row.every((value) => typeof value === "boolean"),
  ),
  "Matrix must contain boolean rows",
);

const decoded: StyledQrDecodedPayload = decodeStyledQrMatrix(modules);

check(decoded.version === matrix.version, "Decoded version differs");
check(
  decoded.errorCorrection === matrix.errorCorrection,
  "Decoded correction level differs",
);
check(decoded.mask >= 0 && decoded.mask <= 7, "Invalid mask");
check(decoded.correctedErrorCount === 0, "Unexpected corrections");
check(decoded.eciAssignment === 26, "Text must declare UTF-8 ECI");
check(decoded.utf8TextOrNull() === text, "Decoded text differs");
sameBytes(decoded.copyBytes(), Array.from(new TextEncoder().encode(text)));

// Returned arrays must not expose the matrix's internal state.
const firstRow = required(modules[0], "First row is missing");
const original = matrix.get(0, 0);
firstRow[0] = !original;

check(matrix.get(0, 0) === original, "Matrix copy mutated the original");

// Unsigned binary bytes must preserve their values and omit implicit ECI.
const sourceBytes = Array.from(new Uint8Array([0, 1, 127, 128, 254, 255]));
const binary = encodeStyledQrBytes(sourceBytes, encoding);
const binaryDecoded = decodeStyledQrMatrix(binary.copyModules());

check(Array.isArray(binaryDecoded.copyBytes()), "Bytes must be number[]");
sameBytes(binaryDecoded.copyBytes(), sourceBytes);
check(binaryDecoded.eciAssignment == null, "Raw bytes must not add ECI");
check(binaryDecoded.utf8TextOrNull() == null, "Raw bytes must not imply text");

// Low-level empty input remains encodable.
const emptyText = decodeStyledQrMatrix(
  encodeStyledQrText("").copyModules(),
);
const emptyBytes = decodeStyledQrMatrix(
  encodeStyledQrBytes([]).copyModules(),
);

check(emptyText.utf8TextOrNull() === "", "Empty text must round-trip");
check(emptyText.eciAssignment === 26, "Empty text must retain ECI");
check(emptyBytes.copyBytes().length === 0, "Empty bytes must round-trip");
check(emptyBytes.eciAssignment == null, "Empty bytes must omit ECI");

expectThrow(() => encodeStyledQrBytes([-1]));
expectThrow(() => encodeStyledQrBytes([256]));
expectThrow(() => encodeStyledQrBytes([1.5]));
expectThrow(() => decodeStyledQrMatrix([[true]]));
expectThrow(() => matrix.get(0.5, 0));

// Full visual settings, including a real rectangular PNG logo.
const logoUri =
  "data:image/png;base64," +
  "iVBORw0KGgoAAAANSUhEUgAAACgAAAAUCAIAAABwJOjsAAAAKUlEQVR4nGP4z8CAB+GTI4QoMnnU4lGLRy0etXjU4lGLRy0etXg4WQwA72wdDlLL0dYAAAAASUVORK5CYII=";

const visual = new StyledQrOptions();
visual.themeId = "coffee";
visual.errorCorrection = "H";
visual.minVersion = 4;
visual.maxVersion = 4;
visual.foregroundHex = "#123C66";
visual.foregroundAlpha = 255;
visual.backgroundHex = "#FFF0CC";
visual.backgroundAlpha = 255;
visual.quietZoneModules = 6;
visual.moduleShape = "rounded-square";
visual.moduleScale = 0.84;
visual.roundedModuleRadiusFraction = 0.31;
visual.preserveFunctionPatterns = true;
visual.functionPatternStyle = "preserve-finders-and-alignment";
visual.finderPatternShape = "circle";
visual.finderOuterShape = "rounded-square";
visual.finderCenterShape = "diamond";
visual.finderOuterColorHex = "#123C66";
visual.finderCenterColorHex = "#0F4C5C";
visual.foregroundGradient = new StyledQrGradientOptions(
  "#075985",
  "#6D28D9",
);

const logo = new StyledQrLogoOptions();
logo.sizeFraction = 0.18;
logo.paddingFraction = 0.04;
logo.backgroundHex = "#DDEEFF";
logo.backgroundAlpha = 255;
logo.backgroundShape = "rounded-square";
logo.cornerRadiusFraction = 0.22;

visual.logo = logo;
visual.logoDataUri = logoUri;

const styledSvg = generateStyledQrSvgWithOptions("x", visual);
check(styledSvg.includes("<linearGradient"), "Gradient is missing");
check(styledSvg.includes("<image"), "Logo is missing");

const output = new StyledQrExportOptions(450);
output.sizeMode = "integer-modules";

const sizedSvg = svgOf(
  renderStyledQrSvgForExport("x", output, visual),
);
check(sizedSvg.includes('width="450"'), "Export dimensions are missing");

const binarySvg: string = binary.toSvg(output, visual);
check(binarySvg.includes("<image"), "Binary QR logo is missing");

// Preserve historical default output.
check(
  generateStyledQrSvg("x") === generateStyledQrSvgWithOptions("x"),
  "Legacy default output changed",
);

// All built-in themes must remain available through both APIs.
const themeIds = getStyledQrThemeIds();

check(themeIds.length === 7, "Expected seven built-in themes");
check(new Set(themeIds).size === 7, "Theme IDs must be unique");

for (const themeId of themeIds) {
  const themed = new StyledQrOptions();
  themed.themeId = themeId;

  check(
    generateStyledQrSvgWithOptions("x", themed) ===
      generateThemedQrSvg("x", themeId),
    "Theme output differs: " + themeId,
  );

  themed.backgroundHex = "#FFFFFF";
  themed.backgroundAlpha = 255;
  themed.foregroundGradient = new StyledQrGradientOptions(
    "#075985",
    "#6D28D9",
  );
  themed.finderOuterShape = "diamond";
  themed.finderCenterColorHex = "#0F4C5C";

  svgOf(renderStyledQrSvg("x", themed));
}

// Explicit reset flags must be callable from TypeScript.
const reset = new StyledQrOptions();
reset.themeId = "aurora";
reset.clearForegroundGradient = true;
reset.clearFinderOuterColor = true;
reset.clearFinderCenterColor = true;

svgOf(renderStyledQrSvg("x", reset));

// Allowed alpha must reach the SVG.
const transparent = new StyledQrOptions();
transparent.backgroundHex = "#FF0000";
transparent.backgroundAlpha = 128;

check(
  svgOf(renderStyledQrSvg("x", transparent)).includes("rgba("),
  "Background alpha is missing",
);

// High-level empty input and programmatically accessible errors.
check(renderStyledQrSvg("").isEmpty, "Blank input must return empty");

const invalidStyle = new StyledQrOptions();
invalidStyle.moduleScale = 0;
expectError(renderStyledQrSvg("", invalidStyle), "InvalidStyle");

expectError(
  renderStyledQrSvgForExport("", new StyledQrExportOptions(0)),
  "InvalidRenderParameters",
);

const tooLong = new StyledQrOptions();
tooLong.minVersion = 1;
tooLong.maxVersion = 1;
expectError(
  renderStyledQrSvg("x".repeat(100), tooLong),
  "PayloadTooLong",
);

const brokenLogo = new StyledQrOptions();
brokenLogo.logoDataUri = "invalid";
expectError(renderStyledQrSvg("x", brokenLogo), "LogoValidationFailed");

expectThrow(() => generateStyledQrSvgWithOptions(""));

// These PNG calls complete before browser-only rendering is needed.
const emptyPngPromise: Promise<StyledQrPngResult> = renderStyledQrPng(
  "",
  new StyledQrExportOptions(300),
);
const emptyPng = await emptyPngPromise;

check(emptyPng.isEmpty, "Blank PNG input must return empty");
check(emptyPng.dataUri == null, "Empty PNG must not contain image data");

const invalidPng = await renderStyledQrPng(
  "",
  new StyledQrExportOptions(0),
);
check(
  invalidPng.error?.code === "InvalidRenderParameters",
  "PNG parameter error is missing",
);

// Compile-only checks: these calls must remain type errors.
function rejectedCalls(encoded: StyledQrMatrix): void {
  // @ts-expect-error Text must be a string.
  encodeStyledQrText(123);

  // @ts-expect-error Bytes must be numbers.
  encodeStyledQrBytes(["1"]);

  // @ts-expect-error Matrix cells must be booleans.
  decodeStyledQrMatrix([[1]]);

  // @ts-expect-error Output must be an export-options object.
  encoded.toSvg(300);
}

void rejectedCalls;

for (const fixture of qrParityFixtures) {
  const options = new StyledQrEncodingOptions();
  options.errorCorrection = fixture.requestedErrorCorrection;
  options.minVersion = fixture.minVersion;
  options.maxVersion = fixture.maxVersion;

  const encoded =
    fixture.text === null
      ? encodeStyledQrBytes(fixture.bytes, options)
      : encodeStyledQrText(fixture.text, options);

  check(encoded.version === fixture.version, fixture.id + ": version");
  check(
    encoded.errorCorrection === fixture.errorCorrection,
    fixture.id + ": correction",
  );
  check(encoded.size === fixture.size, fixture.id + ": size");

  const rowsFrom = (
    read: (row: number, column: number) => boolean,
  ): string[] =>
    Array.from({ length: encoded.size }, (_, row) =>
      Array.from(
        { length: encoded.size },
        (_, column) => (read(row, column) ? "1" : "0"),
      ).join(""),
    );

  check(
    JSON.stringify(rowsFrom((r, c) => encoded.get(r, c))) ===
      JSON.stringify(fixture.moduleRows),
    fixture.id + ": modules",
  );
  check(
    JSON.stringify(rowsFrom((r, c) => encoded.isFunctionModule(r, c))) ===
      JSON.stringify(fixture.functionRows),
    fixture.id + ": function modules",
  );
  check(
    JSON.stringify(rowsFrom((r, c) => encoded.isLocatorModule(r, c))) ===
      JSON.stringify(fixture.locatorRows),
    fixture.id + ": locator modules",
  );

  // Decode the stored matrix, independently of this run's encoder output.
  const payload = decodeStyledQrMatrix(
    fixture.moduleRows.map((row) =>
      Array.from(row, (cell) => cell === "1"),
    ),
  );

  check(payload.version === fixture.version, fixture.id + ": decoded version");
  check(
    payload.errorCorrection === fixture.errorCorrection,
    fixture.id + ": decoded correction",
  );
  check(payload.mask === fixture.mask, fixture.id + ": mask");
  check(payload.correctedErrorCount === 0, fixture.id + ": corrections");
  check(
    (payload.eciAssignment ?? null) === fixture.eciAssignment,
    fixture.id + ": ECI",
  );
  check(
    (payload.utf8TextOrNull() ?? null) === fixture.utf8Text,
    fixture.id + ": decoded text",
  );
  sameBytes(payload.copyBytes(), fixture.bytes);
}

console.log("Frozen QR parity fixtures: PASS");

const scanOptions = new StyledQrScanabilityOptions();
scanOptions.style.foregroundAlpha = 0;
const scanResult = analyzeStyledQrScanability("scanability", scanOptions);
const scanReport = required(scanResult.report, "Expected scanability report");
check(scanReport.isRenderable, "Warnings must not prevent rendering");
check(scanReport.measurements.minContrastRatio == null, "Transparent contrast must be null");
check(scanReport.warnings[0]?.code === "CONTRAST_INDETERMINATE_TRANSPARENCY", "Missing transparency warning");
const contrast: number | null | undefined = scanReport.measurements.minContrastRatio;
check(contrast == null, "Nullable contrast contract");
scanOptions.style.moduleScale = 0.1;
const badScan = analyzeStyledQrScanability("scanability", scanOptions);
check(badScan.report == null && badScan.error?.code === "InvalidStyle", "Invalid style must return an error");

console.log("ESM imports, strict types and runtime API checks: PASS");
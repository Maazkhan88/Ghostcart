import { stat } from "node:fs/promises";
import { fileURLToPath, pathToFileURL } from "node:url";
import path from "node:path";

const CLOUDFLARE_TEST_MODULE =
  "data:text/javascript," +
  encodeURIComponent(
    "export const env = globalThis.__GHOST_CART_TEST_ENV__ ?? {};",
  );

async function existingFile(candidate) {
  try {
    const result = await stat(candidate);
    return result.isFile() ? candidate : null;
  } catch {
    return null;
  }
}

export async function resolve(specifier, context, nextResolve) {
  if (specifier === "cloudflare:workers") {
    return { shortCircuit: true, url: CLOUDFLARE_TEST_MODULE };
  }

  if (
    context.parentURL?.startsWith("file:") &&
    (specifier.startsWith("./") || specifier.startsWith("../")) &&
    path.extname(specifier) === ""
  ) {
    const parentPath = path.dirname(fileURLToPath(context.parentURL));
    const basePath = path.resolve(parentPath, specifier);
    const resolved =
      (await existingFile(`${basePath}.ts`)) ??
      (await existingFile(`${basePath}.tsx`)) ??
      (await existingFile(path.join(basePath, "index.ts")));

    if (resolved) {
      return { shortCircuit: true, url: pathToFileURL(resolved).href };
    }
  }

  return nextResolve(specifier, context);
}

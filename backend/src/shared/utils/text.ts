export function humanizeCategorySlug(slug: string): string {
  return slug
    .split(/[-_]/g)
    .filter((chunk) => chunk.length > 0)
    .map((chunk) => chunk.charAt(0).toUpperCase() + chunk.slice(1))
    .join(" ");
}

export function chunkStrings(items: string[], size: number): string[][] {
  const chunks: string[][] = [];

  for (let index = 0; index < items.length; index += size) {
    chunks.push(items.slice(index, index + size));
  }

  return chunks;
}

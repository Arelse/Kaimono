// Real working novel source backed by Project Gutenberg's public catalog
// (gutenberg.org) — 70,000+ public domain books, free and legal to read.
// Uses their public "Gutendex" API (gutendex.com), a community-run
// read-only mirror of Gutenberg's catalog metadata.

const API = "https://gutendex.com/books";

function toEntry(book) {
  return {
    id: String(book.id),
    title: book.title || "Untitled",
    cover: book.formats && book.formats["image/jpeg"] ? book.formats["image/jpeg"] : "",
    description: (book.subjects || []).join(", "),
    genres: book.subjects || [],
    author: (book.authors || []).map(a => a.name).join(", ") || "Unknown",
    status: "completed", // these are finished, published books
    url: "https://www.gutenberg.org/ebooks/" + book.id,
  };
}

module.popular = async (page) => {
  const res = await httpGet(`${API}/?sort=popular&page=${page}`, { "Accept": "application/json" });
  const json = JSON.parse(res);
  return (json.results || []).map(toEntry);
};

module.latest = async (page) => {
  // Gutendex has no "latest" sort; fall back to popular.
  return module.popular(page);
};

module.search = async (query, page) => {
  const res = await httpGet(`${API}/?search=${encodeURIComponent(query)}&page=${page}`, { "Accept": "application/json" });
  const json = JSON.parse(res);
  return (json.results || []).map(toEntry);
};

module.details = async (id) => {
  const res = await httpGet(`${API}/${id}`, { "Accept": "application/json" });
  const book = JSON.parse(res);
  return toEntry(book);
};

module.chunks = async (id) => {
  // Public domain books are single downloads, not chaptered releases —
  // modeled as one "chapter" that contains the whole book text.
  return [{ id: id, title: "Full text", number: 1, uploadDate: null }];
};

module.pages = async (chunkId) => {
  const res = await httpGet(`${API}/${chunkId}`, { "Accept": "application/json" });
  const book = JSON.parse(res);
  const textUrl = book.formats && book.formats["text/plain; charset=utf-8"]
    ? book.formats["text/plain; charset=utf-8"]
    : book.formats["text/plain"];
  if (!textUrl) return ["No plain-text version available for this book."];
  const raw = await httpGet(textUrl, {});
  // Split into paragraphs on blank lines, trimming Gutenberg's license
  // header/footer boilerplate isn't done here — kept simple for now.
  return raw.split(/\r?\n\r?\n/).map(p => p.trim()).filter(p => p.length > 0);
};

module.streams = async (chunkId) => {
  return [];
};

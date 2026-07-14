import http from "node:http";

const NEXT_PORT = 3001;
const LISTEN_PORT = 3000;
const PREFIX = "/codeeditor/default";

const server = http.createServer((req, res) => {
  // /absports/3000/ 以降を取り出し、Next.js の basePath 付き URL に変換
  const incoming = req.url || "/";
  const stripped = incoming.replace(/^\/absports\/3000/, "");
  const target = `${PREFIX}/absports/3000${stripped}`;

  const options = {
    hostname: "127.0.0.1",
    port: NEXT_PORT,
    path: target,
    method: req.method,
    headers: req.headers,
  };

  const proxyReq = http.request(options, (proxyRes) => {
    res.writeHead(proxyRes.statusCode, proxyRes.headers);
    proxyRes.pipe(res, { end: true });
  });

  proxyReq.on("error", (err) => {
    console.error("Proxy error:", err.message);
    res.writeHead(502);
    res.end("Bad Gateway");
  });

  req.pipe(proxyReq, { end: true });
});

server.listen(LISTEN_PORT, () => {
  console.log(`SageMaker proxy: :${LISTEN_PORT} → :${NEXT_PORT} (prefix: ${PREFIX})`);
});

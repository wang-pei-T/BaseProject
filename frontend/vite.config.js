import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

function preserveBrowserHost(proxy) {
  proxy.on("proxyReq", (proxyReq, req) => {
    const h = req.headers.host;
    if (h) {
      proxyReq.setHeader("Host", h);
    }
  });
}

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "^/(auth|tenant|platform|api)": {
        target: "http://127.0.0.1:8080",
        changeOrigin: false,
        configure: preserveBrowserHost,
      },
    },
  },
});

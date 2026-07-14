import type { NextConfig } from "next";

const isSagemaker = process.env.SAGEMAKER === "1";

const nextConfig: NextConfig = {
  ...(isSagemaker ? { basePath: "/codeeditor/default/absports/3000" } : {}),
  rewrites: async () => [
    {
      source: "/api/:path*",
      destination: "http://localhost:8080/api/:path*",
    },
  ],
};

export default nextConfig;

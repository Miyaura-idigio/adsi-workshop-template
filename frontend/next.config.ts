import type { NextConfig } from "next";

const isSagemaker = process.env.SAGEMAKER === "1";
const basePath = isSagemaker ? "/codeeditor/default/absports/3000" : "";

const nextConfig: NextConfig = {
  basePath: basePath || undefined,
  assetPrefix: basePath || undefined,
  ...(isSagemaker ? { skipTrailingSlashRedirect: true } : {}),
  rewrites: async () => [
    {
      source: "/api/:path*",
      destination: "http://localhost:8080/api/:path*",
    },
  ],
};

export default nextConfig;

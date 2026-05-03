import { useMemo } from "react";
import { ConfigProvider, theme as antdThemeApi } from "antd";
import antdTheme from "../../theme/antd-theme";

function readCssPrimary() {
  if (typeof document === "undefined") {
    return antdTheme.token.colorPrimary;
  }
  const v = getComputedStyle(document.documentElement).getPropertyValue("--primary").trim();
  return v || antdTheme.token.colorPrimary;
}

export default function LoginThemeProvider({ children }) {
  const merged = useMemo(() => {
    const colorPrimary = readCssPrimary();
    return {
      ...antdTheme,
      algorithm: antdThemeApi.darkAlgorithm,
      token: {
        ...antdTheme.token,
        colorPrimary,
        colorInfo: colorPrimary,
        colorBgElevated: "rgba(22, 32, 44, 0.95)",
        colorBgContainer: "rgba(18, 26, 36, 0.85)",
        colorBorder: "rgba(255, 255, 255, 0.12)",
        colorBorderSecondary: "rgba(255, 255, 255, 0.08)",
        colorText: "#e8eef5",
        colorTextSecondary: "rgba(200, 210, 220, 0.78)",
        colorTextTertiary: "rgba(160, 175, 190, 0.65)",
      },
      components: {
        ...antdTheme.components,
        Input: {
          colorBgContainer: "rgba(10, 16, 24, 0.75)",
          activeBorderColor: colorPrimary,
          hoverBorderColor: colorPrimary,
        },
      },
    };
  }, []);
  return <ConfigProvider theme={merged}>{children}</ConfigProvider>;
}

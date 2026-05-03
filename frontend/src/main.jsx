import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { RouterProvider } from "react-router-dom";
import { ConfigProvider, App as AntdApp } from "antd";
import zhCN from "antd/locale/zh_CN";
import "./styles/tokens.css";
import "./styles/layout.css";
import "./index.css";
import "./App.css";
import router from "./router";
import appConfig from "./config/app-config";
import antdTheme from "./theme/antd-theme";

document.title = appConfig.pageTitle;

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <ConfigProvider locale={zhCN} theme={antdTheme}>
      <AntdApp>
        <RouterProvider router={router} />
      </AntdApp>
    </ConfigProvider>
  </StrictMode>,
);

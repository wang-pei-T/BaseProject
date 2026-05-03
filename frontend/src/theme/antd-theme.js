const antdTheme = {
  token: {
    colorPrimary: "#008c8c",
    colorSuccess: "#52c41a",
    colorWarning: "#faad14",
    colorError: "#ff4d4f",
    colorInfo: "#008c8c",
    borderRadius: 4,
    wireframe: false,
  },
  components: {
    Layout: {
      headerBg: "#008c8c",
      headerHeight: 48,
      headerPadding: "0 16px",
    },
    Menu: {
      itemBg: "transparent",
      subMenuItemBg: "transparent",
    },
  },
};

export const darkNeutralChromeOverrides = {
  token: {
    colorBgBase: "#000000",
    colorBgLayout: "#111111",
    colorBgContainer: "#161616",
    colorBgElevated: "#1c1c1c",
  },
  components: {
    Layout: {
      siderBg: "#0a0a0a",
      bodyBg: "#111111",
    },
    Menu: {
      darkItemBg: "transparent",
      darkSubMenuItemBg: "#0a0a0a",
      darkItemHoverBg: "rgba(255, 255, 255, 0.06)",
      popupBg: "#141414",
    },
  },
};

export default antdTheme;
